package org.example.sharedprompts.module.domain.production.service.job.process.recovery;

import com.fasterxml.jackson.databind.ObjectMapper;
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
public class ParsedRecoveryService {

    private final JobStateService jobStateService;
    private final CommandDeserializer commandDeserializer;
    private final AIResponseHandler aiResponseHandler;
    private final ContentRenderer contentRenderer;
    private final ContentFormatter contentFormatter;
    private final ContentStorageService contentStorageService;
    private final StorageStrategyFactory storageStrategyFactory;
    private final FileNameGenerator fileNameGenerator;
    private final ObjectMapper objectMapper;
    private final JobExceptionHandler exceptionHandler;

    public void recover(String jobId) {
        JobEntity job = jobStateService.getJob(jobId);

        if (job.getStatus() != JobStatus.PARSED && job.getStatus() != JobStatus.PARSE_FAILED) {
            throw new RecoveryException("Cannot retry from " + job.getStatus());
        }

        ProductionCommand command = commandDeserializer.deserialize(job);
        ProductionCommandType commandType = command.getCommandType();
        String commandTypeName = commandType.name();

        try {
            String parsedJson;
            if (job.getStatus() == JobStatus.PARSE_FAILED) {
                if (job.getRawResponse() == null || job.getRawResponse().isBlank()) {
                    throw new RecoveryException("Cannot retry: rawResponse is null");
                }
                jobStateService.resetToParsable(jobId);

                ParsedResponse parsedResponse = aiResponseHandler.parse(job.getRawResponse(), commandType);
                jobStateService.markParsed(jobId, parsedResponse.jsonString());
                parsedJson = parsedResponse.jsonString();
            } else {
                parsedJson = job.getParsedResponse();
            }

            com.fasterxml.jackson.databind.JsonNode parsedJsonNode = objectMapper.readTree(parsedJson);
            String renderedContent = contentRenderer.render(parsedJsonNode, commandType);
            jobStateService.markRendered(jobId, renderedContent);

            String outputFormat = command.getOutputFormat();
            String baseFileName = fileNameGenerator.generate(command);
            var converted = contentFormatter.format(renderedContent, outputFormat, baseFileName);

            String filePath = contentStorageService.store(
                    converted.data(), converted.contentType(), job, converted.fileName());
            StorageStrategy storageStrategy = storageStrategyFactory.getStorageStrategy();
            jobStateService.markStored(jobId, filePath, storageStrategy);

            jobStateService.markCompleted(jobId);
            log.info("Job recovered from PARSED successfully - jobId: {}", jobId);
        } catch (Exception e) {
            log.error("Recovery failed from PARSED - jobId: {}", jobId, e);
            exceptionHandler.handleRecoveryException(jobId, commandTypeName, new RecoveryException("Recovery failed", e));
        }
    }
}

