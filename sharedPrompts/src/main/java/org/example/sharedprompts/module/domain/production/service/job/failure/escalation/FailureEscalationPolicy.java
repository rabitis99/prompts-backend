package org.example.sharedprompts.module.domain.production.service.job.failure.escalation;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class FailureEscalationPolicy {
    
    private static final Set<String> PERMANENT_FAILURE_REASONS = Set.of(
            "PARSE_FAILED",
            "VALIDATION_ERROR",
            "INVALID_INPUT"
    );
    
    /**
     * @param jobId reserved for future per-job or per-tenant policy
     * @param failureReason reason code to classify as permanent or retriable
     */
    public boolean isPermanentFailure(String jobId, String failureReason) {
        return failureReason != null && PERMANENT_FAILURE_REASONS.contains(failureReason);
    }
}

