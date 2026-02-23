package org.example.sharedprompts.module.domain.production.service.job.process;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.executor.literary.LiteraryCommand;
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
import org.example.sharedprompts.module.domain.production.service.literary.LiteraryAIExecutor;
import org.example.sharedprompts.module.domain.production.service.literary.LiteraryGenerationStrategy;
import org.example.sharedprompts.module.domain.production.service.literary.LiteraryGenerationStrategyRegistry;
import org.example.sharedprompts.module.domain.production.service.literary.pipeline.LiteraryOutputPipeline;
import org.example.sharedprompts.module.domain.production.service.literary.pipeline.LiteraryPipelineResult;
import org.example.sharedprompts.module.domain.production.service.literary.validation.LiteraryValidationResult;
import org.example.sharedprompts.module.domain.production.service.literary.validation.LiteraryValidatorRegistry;
import org.example.sharedprompts.module.domain.production.service.parser.ParsedResponse;
import org.example.sharedprompts.module.domain.production.service.prompt.PromptTemplateService;
import org.example.sharedprompts.module.domain.production.util.TenantContextValidator;
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
    private final Optional<JobGateLockService> jobGateLockService;
    private final LiteraryGenerationStrategyRegistry literaryStrategyRegistry;
    private final LiteraryValidatorRegistry literaryValidatorRegistry;
    private final LiteraryOutputPipeline literaryOutputPipeline;
    private final LiteraryAIExecutor literaryAIExecutor;

    public void processJob(String jobId) {
        Instant startTime = Instant.now();

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

        String tenantId = TenantContextValidator.requireTenantContextForJob(jobId);
        boolean gateLockAcquired = jobGateLockService
                .map(s -> s.tryLock(tenantId, jobId))
                .orElse(true);
        if (!gateLockAcquired) {
            log.info("Job gate lock not acquired - jobId: {} (will retry via queue)", jobId);
            return;
        }
        try {
            doProcessJob(jobId, job, startTime, tenantId);
        } finally {
            jobGateLockService.ifPresent(s -> s.unlock(tenantId, jobId));
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
                    command,
                    job.getUserInput()
            );

            jobStateService.updateJobPromptVersion(job.getJobId(), mergedPrompt.version());

            if (command.getCommandType() == ProductionCommandType.LITERARY) {
                processLiteraryJob(jobId, job, (LiteraryCommand) command, mergedPrompt.content(), startTime, commandType);
                return;
            }

            AIJobExecutor.AIExecutionResult aiResult = aiJobExecutor.execute(job, command, mergedPrompt.content());
            jobStateService.setModelInfo(job.getJobId(), aiResult.modelName(), aiResult.tokenUsage());

            ParsedResponse parsedResponse = aiResponseHandler.parse(aiResult.rawResponse(), command.getCommandType());
            String renderedContent = contentRenderer.render(parsedResponse.jsonNode(), command.getCommandType());

            String s3Key;
            if (command.getCommandType() == ProductionCommandType.IMAGE) {
                s3Key = renderedContent;
                log.info("Using existing image path for IMAGE command - s3Key: {}", s3Key);
            } else {
                String outputFormat = command.getOutputFormat();
                String baseFileName = fileNameGenerator.generate(command);
                var converted = contentFormatter.format(renderedContent, outputFormat, baseFileName);
                s3Key = contentStorageService.store(converted.data(), converted.contentType(), job, converted.fileName());
            }
            jobStateService.markStored(job.getJobId(), s3Key);

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

    private static final int LITERARY_VALIDATION_MAX_RETRIES = 2;

    private void processLiteraryJob(String jobId, JobEntity job, LiteraryCommand command, String composedPrompt,
                                    Instant startTime, String commandType) {
        LiteraryGenerationStrategy strategy = literaryStrategyRegistry.getStrategy(command.literaryType());
        var validator = literaryValidatorRegistry.getValidator(command.literaryType());

        String content = null;
        for (int attempt = 0; attempt <= LITERARY_VALIDATION_MAX_RETRIES; attempt++) {
            content = strategy.generate(job, command, composedPrompt, literaryAIExecutor);
            LiteraryValidationResult result = validator.validate(content);
            if (result.isValid()) {
                break;
            }
            log.warn("Literary validation failed - jobId: {}, attempt: {}, errors: {}", jobId, attempt + 1, result.getErrors());
            if (attempt == LITERARY_VALIDATION_MAX_RETRIES) {
                throw new ContentRenderException("Literary output validation failed after " + (LITERARY_VALIDATION_MAX_RETRIES + 1) + " attempts: " + result.getErrors());
            }
        }

        LiteraryPipelineResult pipelineResult = literaryOutputPipeline.run(content, job);
        jobStateService.markStoredLiterary(
                job.getJobId(),
                pipelineResult.getOriginalTxtKey(),
                pipelineResult.getPreviewHtmlKey(),
                pipelineResult.getFinalPdfKey()
        );

        Duration duration = Duration.between(startTime, Instant.now());
        jobMetrics.recordJobCompleted(commandType, duration);
        log.info("Literary job completed - jobId: {}, duration: {}ms", jobId, duration.toMillis());
    }
}
