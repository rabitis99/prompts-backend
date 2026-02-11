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
import org.example.sharedprompts.module.domain.production.service.job.process.execution.content.ContentFormatter;
import org.example.sharedprompts.module.domain.production.service.job.process.execution.content.ContentStorageService;
import org.example.sharedprompts.module.domain.production.service.job.process.util.CommandDeserializer;
import org.example.sharedprompts.module.domain.production.service.job.process.util.FileNameGenerator;
import org.example.sharedprompts.module.domain.production.service.storage.StorageStrategy;
import org.example.sharedprompts.module.domain.production.service.storage.StorageStrategyFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RenderedRecoveryService {

    private final JobStateService jobStateService;
    private final CommandDeserializer commandDeserializer;
    private final ContentFormatter contentFormatter;
    private final ContentStorageService contentStorageService;
    private final StorageStrategyFactory storageStrategyFactory;
    private final FileNameGenerator fileNameGenerator;
    private final JobExceptionHandler exceptionHandler;

    public void recover(String jobId) {
        JobEntity job = jobStateService.getJob(jobId);

        if (job.getStatus() != JobStatus.RENDERED) {
            throw new RecoveryException("Cannot retry from RENDERED: current status is " + job.getStatus());
        }

        if (job.getAiGeneratedContent() == null || job.getAiGeneratedContent().isBlank()) {
            throw new RecoveryException("Cannot retry: aiGeneratedContent is null");
        }

        ProductionCommand command = commandDeserializer.deserialize(job);
        ProductionCommandType commandType = command.getCommandType();
        String commandTypeName = commandType.name();

        try {
            String outputFormat = command.getOutputFormat();
            String baseFileName = fileNameGenerator.generate(command);
            var converted = contentFormatter.format(job.getAiGeneratedContent(), outputFormat, baseFileName);

            String filePath = contentStorageService.store(
                    converted.data(), converted.contentType(), job, converted.fileName());
            StorageStrategy storageStrategy = storageStrategyFactory.getStorageStrategy();
            jobStateService.markStored(jobId, filePath, storageStrategy);

            jobStateService.markCompleted(jobId);
            log.info("Job recovered from RENDERED successfully - jobId: {}", jobId);
        } catch (Exception e) {
            log.error("Recovery failed from RENDERED - jobId: {}", jobId, e);
            exceptionHandler.handleRecoveryException(jobId, commandTypeName, new RecoveryException("Recovery failed", e));
        }
    }
}

