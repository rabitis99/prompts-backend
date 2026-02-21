package org.example.sharedprompts.module.domain.production.service.ai.retry;

public interface BackoffPolicy {
    long calculateDelayMs(int attempt);
}

