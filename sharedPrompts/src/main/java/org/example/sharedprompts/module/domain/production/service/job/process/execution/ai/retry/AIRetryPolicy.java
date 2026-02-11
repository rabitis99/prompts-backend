package org.example.sharedprompts.module.domain.production.service.job.process.execution.ai.retry;

import org.springframework.stereotype.Component;

@Component
public class AIRetryPolicy {

    private static final long MAX_BACKOFF_MS = 3000;
    private static final long INITIAL_BACKOFF_MS = 500;

    public long calculateBackoff(int retryCount) {
        long backoffMs = (long) (INITIAL_BACKOFF_MS * Math.pow(2, retryCount - 1));
        return Math.min(backoffMs, MAX_BACKOFF_MS);
    }

    public void sleep(long backoffMs) {
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}

