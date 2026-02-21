package org.example.sharedprompts.module.domain.production.service.job.failure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.service.job.failure.metrics.JobFailureMetricsRecorder;
import org.example.sharedprompts.module.domain.production.service.job.failure.escalation.FailureEscalationPolicy;
import org.example.sharedprompts.module.domain.production.service.job.JobStateService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobFailureHandler {
    
    private final JobStateService jobStateService;
    private final JobFailureMetricsRecorder metricsRecorder;
    private final FailureEscalationPolicy escalationPolicy;
    
    public void handleFailure(String jobId, String commandType, String failureReason, Exception exception) {
        String errorMessage = exception != null && exception.getMessage() != null
                ? exception.getMessage()
                : failureReason;
        try {
            jobStateService.saveJobFailure(jobId, errorMessage);
        } catch (Exception e) {
            log.error("Failed to persist job failure state - jobId: {}", jobId, e);
        }

        try {
            metricsRecorder.recordFailure(jobId, commandType, failureReason);
        } catch (Exception e) {
            log.error("Failed to record failure metrics - jobId: {}", jobId, e);
        }

        if (escalationPolicy.isPermanentFailure(jobId, failureReason)) {
            log.error("Job permanent failure - jobId: {}, commandType: {}, reason: {}",
                    jobId, commandType, failureReason, exception);
        }
    }
}

