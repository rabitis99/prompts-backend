package org.example.sharedprompts.module.domain.production.service.job.process;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;
import org.example.sharedprompts.module.domain.production.model.tenant.TenantContext;
import org.example.sharedprompts.module.domain.production.service.job.JobLockService;
import org.example.sharedprompts.module.domain.production.service.job.JobStateService;
import org.example.sharedprompts.module.domain.production.service.job.metrics.JobMetrics;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.AIServiceException;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.ContentRenderException;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.JobExceptionHandler;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.RecoveryException;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.StorageException;
import org.example.sharedprompts.module.domain.production.service.job.process.execution.ai.AIJobExecutor;
import org.example.sharedprompts.module.domain.production.service.job.process.execution.ai.AIResponseHandler;
import org.example.sharedprompts.module.domain.production.service.job.process.execution.content.ContentFormatter;
import org.example.sharedprompts.module.domain.production.service.job.process.execution.content.ContentRenderer;
import org.example.sharedprompts.module.domain.production.service.job.process.execution.content.ContentStorageService;
import org.example.sharedprompts.module.domain.production.service.job.process.util.CommandDeserializer;
import org.example.sharedprompts.module.domain.production.service.job.process.util.FileNameGenerator;
import org.example.sharedprompts.module.domain.production.service.parser.ParsedResponse;
import org.example.sharedprompts.module.domain.production.service.prompt.PromptTemplateService;
import org.example.sharedprompts.module.domain.production.service.storage.StorageStrategy;
import org.example.sharedprompts.module.domain.production.service.storage.StorageStrategyFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobProcessor {

    private final JobStateService jobStateService;
    private final JobLockService jobLockService;
    private final PromptTemplateService promptTemplateService;
    private final AIJobExecutor aiJobExecutor;
    private final AIResponseHandler aiResponseHandler;
    private final ContentRenderer contentRenderer;
    private final ContentFormatter contentFormatter;
    private final ContentStorageService contentStorageService;
    private final StorageStrategyFactory storageStrategyFactory;
    private final JobExceptionHandler exceptionHandler;
    private final CommandDeserializer commandDeserializer;
    private final FileNameGenerator fileNameGenerator;
    private final JobMetrics jobMetrics;

    @Async
    public void processJobAsync(String jobId) {
        Instant startTime = Instant.now();
        String commandType = "UNKNOWN";

        try {
            log.info("Processing job - jobId: {}", jobId);

            Optional<JobEntity> lockResult = jobLockService.acquireJobLock(jobId);
            if (lockResult.isEmpty()) {
                log.info("Could not acquire lock for job - jobId: {} (another thread is processing)", jobId);
                return;
            }

            JobEntity job = lockResult.get();
            if (job.isFinalState()) {
                log.info("Job already finished - jobId: {}, status: {}", jobId, job.getStatus());
                return;
            }

            // Ensure tenant context is set at the beginning of async processing
            // In multi-tenant SaaS, tenant_id is REQUIRED for all operations
            // tenant_id must come from TenantContext (set by SecurityContextTaskDecorator from X-Tenant-Id header)
            // DO NOT create or generate tenant_id - it must be provided in the request header
            String tenantId = TenantContext.getCurrentTenantId();
            if (tenantId == null || tenantId.isBlank()) {
                log.error("Tenant context is REQUIRED but not available - jobId: {}, thread: {}. " +
                        "Please ensure X-Tenant-Id header is provided when creating jobs.", 
                        jobId, Thread.currentThread().getName());
                // Throw exception early to fail fast rather than failing later
                throw new IllegalStateException(
                        "Tenant context is required but not set in async thread for jobId: " + jobId + 
                        ". X-Tenant-Id header must be provided when creating jobs.");
            }
            log.debug("Tenant context verified - jobId: {}, tenantId: {}", jobId, tenantId);

            ProductionCommand command = commandDeserializer.deserialize(job);
            commandType = command.getCommandType().name();

            var mergedPrompt = promptTemplateService.mergePrompt(
                    job.getPromptId(),
                    job.getUserId(),
                    command.getCommandType(),
                    job.getUserInput()
            );

            jobStateService.updateJobPromptVersion(job.getJobId(), mergedPrompt.version());

            AIJobExecutor.AIExecutionResult aiResult = aiJobExecutor.execute(job, command, mergedPrompt.content());
            // AI 모델 정보 설정
            jobStateService.setModelInfo(job.getJobId(), aiResult.modelName(), aiResult.tokenUsage());

            ParsedResponse parsedResponse = aiResponseHandler.parse(aiResult.rawResponse(), command.getCommandType());
            String renderedContent = contentRenderer.render(parsedResponse.jsonNode(), command.getCommandType());

            String filePath;
            StorageStrategy storageStrategy = storageStrategyFactory.getStorageStrategy();
            
            // 이미지 생성인 경우 이미 저장된 PNG 파일 경로를 그대로 사용
            if (command.getCommandType() == ProductionCommandType.IMAGE) {
                // ImageRenderer가 이미지 경로를 그대로 반환하므로 그대로 사용
                filePath = renderedContent;
                log.info("Using existing image path for IMAGE command - filePath: {}", filePath);
            } else {
                // 다른 타입은 기존 로직대로 포맷 변환 후 저장
                String outputFormat = command.getOutputFormat();
                String baseFileName = fileNameGenerator.generate(command);
                var converted = contentFormatter.format(renderedContent, outputFormat, baseFileName);
                filePath = contentStorageService.store(converted.data(), converted.contentType(), job, converted.fileName());
            }
            
            jobStateService.markStored(job.getJobId(), filePath, storageStrategy);

            jobStateService.markCompleted(job.getJobId());

            Duration duration = Duration.between(startTime, Instant.now());
            jobMetrics.recordJobCompleted(commandType, duration);
            log.info("Job completed successfully - jobId: {}, duration: {}ms", jobId, duration.toMillis());

        } catch (AIServiceException e) {
            exceptionHandler.handleAIException(jobId, commandType, e);
        } catch (ContentRenderException e) {
            exceptionHandler.handleRenderException(jobId, commandType, e);
        } catch (StorageException e) {
            exceptionHandler.handleStorageException(jobId, commandType, e);
        } catch (Exception e) {
            exceptionHandler.handleGeneralException(jobId, commandType, e);
        }
    }

    /**
     * Job 복구 (FAILED 상태에서 재시도)
     * 
     * 설계 원칙:
     * - 중간 상태 복구 제거
     * - FAILED 상태에서만 retry()를 통해 처음부터 다시 처리
     */
    @Async
    public void recoverJob(String jobId) {
        JobEntity job = jobStateService.getJob(jobId);
        JobStatus status = job.getStatus();

        // Ensure tenant context is set for recovery process
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            log.error("Tenant context is REQUIRED but not available for recovery - jobId: {}, thread: {}. " +
                    "Please ensure X-Tenant-Id header is provided.", 
                    jobId, Thread.currentThread().getName());
            throw new IllegalStateException(
                    "Tenant context is required but not set in async thread for recovery jobId: " + jobId + 
                    ". X-Tenant-Id header must be provided.");
        }

        // FAILED 상태에서만 복구 가능
        if (status != JobStatus.FAILED) {
            log.warn("Cannot recover job - only FAILED status can be recovered. jobId: {}, status: {}", jobId, status);
            return;
        }

        log.info("Recovering failed job - jobId: {}, status: {}", jobId, status);

        try {
            // retry()를 통해 FAILED → PENDING으로 전이 후 처음부터 다시 처리
            jobStateService.retryJob(jobId);
            // 처음부터 다시 처리
            processJobAsync(jobId);
        } catch (Exception e) {
            log.error("Failed to recover job - jobId: {}, status: {}", jobId, status, e);
            ProductionCommand command = commandDeserializer.deserialize(job);
            exceptionHandler.handleRecoveryException(jobId, command.getCommandType().name(), 
                    new RecoveryException("Recovery failed", e));
        }
    }
}

