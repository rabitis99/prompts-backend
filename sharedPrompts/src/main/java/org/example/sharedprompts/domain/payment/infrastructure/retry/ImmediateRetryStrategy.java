package org.example.sharedprompts.domain.payment.infrastructure.retry;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.properties.RetryProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ImmediateRetryStrategy implements RetryStrategy {

    private final RetryPolicy retryPolicy;

    public ImmediateRetryStrategy(RetryProperties retryProperties) {
        this.retryPolicy = RetryPolicy.immediateRetryPolicy();
    }

    @Override
    public boolean shouldRetry(Exception exception, int attemptCount) {
        return retryPolicy.shouldRetry(exception, attemptCount);
    }

    @Override
    public long calculateDelay(int attemptCount) {
        return retryPolicy.calculateDelay(attemptCount);
    }

    @Override
    public int getMaxAttempts() {
        return retryPolicy.getMaxAttempts();
    }
}

