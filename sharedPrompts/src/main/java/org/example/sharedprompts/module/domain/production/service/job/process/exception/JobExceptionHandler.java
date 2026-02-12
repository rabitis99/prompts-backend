package org.example.sharedprompts.module.domain.production.service.job.process.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.exception.ParseException;
import org.example.sharedprompts.module.domain.production.service.job.JobStateService;
import org.example.sharedprompts.module.domain.production.service.job.metrics.JobMetrics;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobExceptionHandler {

    private final JobStateService jobStateService;
    private final JobMetrics jobMetrics;

    public void handleAIException(String jobId, String commandType, AIServiceException e) {
        log.error("AI service exception - jobId: {}, commandType: {}, errorCode: {}", 
                jobId, commandType, e.getErrorCode(), e);
        jobStateService.saveJobFailure(jobId, "AI call failed: " + e.getMessage());
        jobMetrics.recordJobFailed(commandType, "AI_CALL_FAILED");
    }

    public void handleParseException(String jobId, String commandType, ParseException e) {
        log.error("Parse exception - jobId: {}, commandType: {}, errorCode: {}",
                jobId, commandType, e.getErrorCode(), e);
        jobStateService.markParseFailed(jobId, e.getMessage());
        jobMetrics.recordJobFailed(commandType, "PARSE_FAILED");
    }

    public void handleRenderException(String jobId, String commandType, ContentRenderException e) {
        log.error("Render exception - jobId: {}, commandType: {}, errorCode: {}", 
                jobId, commandType, e.getErrorCode(), e);
        jobStateService.saveJobFailure(jobId, "Rendering failed: " + e.getMessage());
        jobMetrics.recordJobFailed(commandType, "RENDER_FAILED");
    }

    public void handleStorageException(String jobId, String commandType, StorageException e) {
        log.error("Storage exception - jobId: {}, commandType: {}, errorCode: {}", 
                jobId, commandType, e.getErrorCode(), e);
        jobStateService.saveJobFailure(jobId, "Storage failed: " + e.getMessage());
        jobMetrics.recordJobFailed(commandType, "STORAGE_FAILED");
    }

    public void handleRecoveryException(String jobId, String commandType, RecoveryException e) {
        log.error("Recovery exception - jobId: {}, commandType: {}, errorCode: {}", 
                jobId, commandType, e.getErrorCode(), e);
        jobStateService.saveJobFailure(jobId, "Recovery failed: " + e.getMessage());
        jobMetrics.recordJobFailed(commandType, "RECOVERY_FAILED");
    }

    public void handleGeneralException(String jobId, String commandType, Exception e) {
        if (e instanceof JobProcessingException jobException) {
            log.error("General exception - jobId: {}, commandType: {}, errorCode: {}",
                    jobId, commandType, jobException.getErrorCode(), e);
        } else {
            log.error("General exception - jobId: {}, commandType: {}", jobId, commandType, e);
        }
        jobStateService.saveJobFailure(jobId, "Job processing failed: " + e.getMessage());
        jobMetrics.recordJobFailed(commandType, "PROCESSING_FAILED");
    }
}

