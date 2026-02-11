package org.example.sharedprompts.module.domain.production.service.job.process;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;
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
import org.example.sharedprompts.module.domain.production.service.job.process.recovery.JobRecoveryService;
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
    private final JobRecoveryService jobRecoveryService;
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
            jobStateService.markAiCalled(job.getJobId(), aiResult.rawResponse(), aiResult.modelName(), aiResult.tokenUsage());

            ParsedResponse parsedResponse = aiResponseHandler.parse(aiResult.rawResponse(), command.getCommandType());
            jobStateService.markParsed(job.getJobId(), parsedResponse.jsonString());

            String renderedContent = contentRenderer.render(parsedResponse.jsonNode(), command.getCommandType());
            jobStateService.markRendered(job.getJobId(), renderedContent);

            String outputFormat = command.getOutputFormat();
            String baseFileName = fileNameGenerator.generate(command);
            var converted = contentFormatter.format(renderedContent, outputFormat, baseFileName);

            String filePath = contentStorageService.store(converted.data(), converted.contentType(), job, converted.fileName());
            StorageStrategy storageStrategy = storageStrategyFactory.getStorageStrategy();
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

    @Async
    public void recoverJob(String jobId) {
        JobEntity job = jobStateService.getJob(jobId);
        JobStatus status = job.getStatus();

        log.info("Recovering job from status - jobId: {}, status: {}", jobId, status);

        try {
            switch (status) {
                case AI_CALLED -> jobRecoveryService.recoverFromAiCalled(jobId);
                case PARSED -> jobRecoveryService.recoverFromParsed(jobId);
                case RENDERED -> jobRecoveryService.recoverFromRendered(jobId);
                case STORED -> jobRecoveryService.recoverFromStored(jobId);
                default -> {
                    log.warn("Cannot recover job from status - jobId: {}, status: {}", jobId, status);
                }
            }
        } catch (Exception e) {
            log.error("Failed to recover job - jobId: {}, status: {}", jobId, status, e);
            ProductionCommand command = commandDeserializer.deserialize(job);
            exceptionHandler.handleRecoveryException(jobId, command.getCommandType().name(), 
                    new RecoveryException("Recovery failed", e));
        }
    }
}

