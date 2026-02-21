package org.example.sharedprompts.module.domain.production.service.job.process;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.service.job.JobLockService;
import org.example.sharedprompts.module.domain.production.service.job.JobStateService;
import org.example.sharedprompts.module.domain.production.service.job.gate.JobGateLockService;
import org.example.sharedprompts.module.domain.production.service.job.metrics.JobMetrics;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.AIServiceException;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.ContentRenderException;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.JobExceptionHandler;
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
import org.example.sharedprompts.module.domain.production.util.TenantContextValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobProcessorDelegate {

    private final JobStateService jobStateService;
    private final JobLockService jobLockService;
    private final PromptTemplateService promptTemplateService;
    private final AIJobExecutor aiJobExecutor;
    private final AIResponseHandler aiResponseHandler;
    private final ContentRenderer contentRenderer;
    private final ContentFormatter contentFormatter;
    private final ContentStorageService contentStorageService;
    private final JobExceptionHandler exceptionHandler;
    private final CommandDeserializer commandDeserializer;
    private final FileNameGenerator fileNameGenerator;
    private final JobMetrics jobMetrics;
    @Autowired(required = false)
    private JobGateLockService jobGateLockService;

    /**
     * Job 처리 실행
     */
    public void processJob(String jobId) {
        Instant startTime = Instant.now();

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
            String tenantId = TenantContextValidator.requireTenantContextForJob(jobId);

            // Gate Lock: 외부 호출(AI/S3) 중복 방지. 획득 실패 시 재큐를 위해 처리하지 않고 반환 (DEPLOYMENT_ISSUES 4.1)
            if (jobGateLockService != null) {
                if (!jobGateLockService.tryLock(tenantId, jobId)) {
                    log.info("Job gate lock not acquired - jobId: {} (will retry via queue)", jobId);
                    return;
                }
            }
            try {
                doProcessJob(jobId, job, startTime, tenantId);
            } finally {
                if (jobGateLockService != null) {
                    jobGateLockService.unlock(tenantId, jobId);
                }
            }
        } finally {
            // 예외는 doProcessJob 내부에서 한 번만 핸들링되며, 여기서는 전파만 함
        }
    }

    private void doProcessJob(String jobId, JobEntity job, Instant startTime, String tenantId) {
        String commandType = "UNKNOWN";
        try {
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

            String s3Key;
            
            // 이미지 생성인 경우 이미 저장된 PNG 파일 경로를 그대로 사용
            if (command.getCommandType() == ProductionCommandType.IMAGE) {
                // ImageRenderer가 이미지 경로를 그대로 반환하므로 그대로 사용
                s3Key = renderedContent;
                log.info("Using existing image path for IMAGE command - s3Key: {}", s3Key);
            } else {
                // 다른 타입은 기존 로직대로 포맷 변환 후 저장
                String outputFormat = command.getOutputFormat();
                String baseFileName = fileNameGenerator.generate(command);
                var converted = contentFormatter.format(renderedContent, outputFormat, baseFileName);
                s3Key = contentStorageService.store(converted.data(), converted.contentType(), job, converted.fileName());
            }
            
            jobStateService.markStored(job.getJobId(), s3Key);
            // markStored() 내부에서 이미 complete()를 호출하므로 중복 호출 제거

            Duration duration = Duration.between(startTime, Instant.now());
            jobMetrics.recordJobCompleted(commandType, duration);
            log.info("Job completed successfully - jobId: {}, duration: {}ms", jobId, duration.toMillis());
        } catch (AIServiceException e) {
            exceptionHandler.handleAIException(jobId, commandType, e);
            throw e;
        } catch (ContentRenderException e) {
            exceptionHandler.handleRenderException(jobId, commandType, e);
            throw e;
        } catch (StorageException e) {
            exceptionHandler.handleStorageException(jobId, commandType, e);
            throw e;
        } catch (Exception e) {
            exceptionHandler.handleGeneralException(jobId, commandType, e);
            throw e;
        }
    }
}

