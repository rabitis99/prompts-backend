package org.example.sharedprompts.module.domain.production.service.job.process.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.exception.ParseException;
import org.example.sharedprompts.module.domain.production.service.job.failure.JobFailureHandler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobExceptionHandler {

    private final JobFailureHandler failureHandler;

    public void handleAIException(String jobId, String commandType, AIServiceException e) {
        failureHandler.handleFailure(jobId, commandType, "AI_CALL_FAILED", e);
    }

    public void handleParseException(String jobId, String commandType, ParseException e) {
        failureHandler.handleFailure(jobId, commandType, "PARSE_FAILED", e);
    }

    public void handleRenderException(String jobId, String commandType, ContentRenderException e) {
        failureHandler.handleFailure(jobId, commandType, "RENDER_FAILED", e);
    }

    public void handleStorageException(String jobId, String commandType, StorageException e) {
        failureHandler.handleFailure(jobId, commandType, "STORAGE_FAILED", e);
    }

    public void handleRecoveryException(String jobId, String commandType, RecoveryException e) {
        failureHandler.handleFailure(jobId, commandType, "RECOVERY_FAILED", e);
    }

    public void handleGeneralException(String jobId, String commandType, Exception e) {
        failureHandler.handleFailure(jobId, commandType, "PROCESSING_FAILED", e);
    }
}

