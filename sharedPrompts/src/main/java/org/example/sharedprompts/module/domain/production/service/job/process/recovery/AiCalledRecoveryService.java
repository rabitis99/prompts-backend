package org.example.sharedprompts.module.domain.production.service.job.process.recovery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;
import org.example.sharedprompts.module.domain.production.service.job.JobStateService;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.JobExceptionHandler;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.RecoveryException;
import org.example.sharedprompts.module.domain.production.service.job.process.execution.ai.AIResponseHandler;
import org.example.sharedprompts.module.domain.production.service.job.process.execution.content.ContentFormatter;
import org.example.sharedprompts.module.domain.production.service.job.process.execution.content.ContentRenderer;
import org.example.sharedprompts.module.domain.production.service.job.process.execution.content.ContentStorageService;
import org.example.sharedprompts.module.domain.production.service.job.process.util.CommandDeserializer;
import org.example.sharedprompts.module.domain.production.service.job.process.util.FileNameGenerator;
import org.example.sharedprompts.module.domain.production.service.parser.ParsedResponse;
import org.example.sharedprompts.module.domain.production.service.storage.StorageStrategy;
import org.example.sharedprompts.module.domain.production.service.storage.StorageStrategyFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiCalledRecoveryService {

    private final JobStateService jobStateService;
    private final CommandDeserializer commandDeserializer;
    private final AIResponseHandler aiResponseHandler;
    private final ContentRenderer contentRenderer;
    private final ContentFormatter contentFormatter;
    private final ContentStorageService contentStorageService;
    private final StorageStrategyFactory storageStrategyFactory;
    private final FileNameGenerator fileNameGenerator;
    private final JobExceptionHandler exceptionHandler;

    public void recover(String jobId) {
        JobEntity job = jobStateService.getJob(jobId);

        if (job.getStatus() != JobStatus.AI_CALLED) {
            throw new RecoveryException("Cannot retry from AI_CALLED: current status is " + job.getStatus());
        }

        if (job.getRawResponse() == null || job.getRawResponse().isBlank()) {
            throw new RecoveryException("Cannot retry: rawResponse is null");
        }

        ProductionCommand command = commandDeserializer.deserialize(job);
        ProductionCommandType commandType = command.getCommandType();
        String commandTypeName = commandType.name();

        try {
            ParsedResponse parsedResponse = aiResponseHandler.parse(job.getRawResponse(), commandType);
            jobStateService.markParsed(jobId, parsedResponse.jsonString());

            String renderedContent = contentRenderer.render(parsedResponse.jsonNode(), commandType);
            jobStateService.markRendered(jobId, renderedContent);

            String outputFormat = command.getOutputFormat();
            String baseFileName = fileNameGenerator.generate(command);
            var converted = contentFormatter.format(renderedContent, outputFormat, baseFileName);

            String filePath = contentStorageService.store(
                    converted.data(), converted.contentType(), job, converted.fileName());
            StorageStrategy storageStrategy = storageStrategyFactory.getStorageStrategy();
            jobStateService.markStored(jobId, filePath, storageStrategy);

            jobStateService.markCompleted(jobId);
            log.info("Job recovered from AI_CALLED successfully - jobId: {}", jobId);
        } catch (Exception e) {
            log.error("Recovery failed from AI_CALLED - jobId: {}", jobId, e);
            exceptionHandler.handleRecoveryException(jobId, commandTypeName, new RecoveryException("Recovery failed", e));
        }
    }
}

