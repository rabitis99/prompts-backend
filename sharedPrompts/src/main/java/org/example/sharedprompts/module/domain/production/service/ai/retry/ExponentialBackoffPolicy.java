package org.example.sharedprompts.module.domain.production.service.ai.retry;

import org.springframework.stereotype.Component;

@Component
public class ExponentialBackoffPolicy implements BackoffPolicy {
    
    private static final long INITIAL_DELAY_MS = 1000L;
    private static final long MAX_DELAY_MS = 30000L;
    
    @Override
    public long calculateDelayMs(int attempt) {
        if (attempt <= 0) {
            return INITIAL_DELAY_MS;
        }
        if (attempt >= 63) {
            return MAX_DELAY_MS;
        }
        long delayMs = INITIAL_DELAY_MS * (1L << attempt);
        return Math.min(delayMs, MAX_DELAY_MS);
    }
}

