package org.example.sharedprompts.domain.payment.infrastructure.retry;

import lombok.Builder;
import lombok.Getter;

import java.util.function.Predicate;

@Getter
@Builder
public class RetryPolicy {

    private final int maxAttempts;
    private final long initialDelayMs;
    private final double backoffMultiplier;
    private final long maxDelayMs;
    private final Predicate<Exception> retryableExceptionPredicate;

    public static RetryPolicy immediateRetryPolicy() {
        return RetryPolicy.builder()
                .maxAttempts(2)
                .initialDelayMs(200)
                .backoffMultiplier(1.0)
                .maxDelayMs(200)
                .retryableExceptionPredicate(e -> {
                    String message = e.getMessage();
                    if (message == null) {
                        return false;
                    }
                    String lowerMessage = message.toLowerCase();
                    return lowerMessage.contains("timeout") || lowerMessage.contains("connection") ||
                            lowerMessage.contains("network") || lowerMessage.contains("unavailable") ||
                            lowerMessage.contains("temporary") || lowerMessage.contains("retry");
                })
                .build();
    }

    public long calculateDelay(int attemptCount) {
        long delay = (long) (initialDelayMs * Math.pow(backoffMultiplier, attemptCount));
        return Math.min(delay, maxDelayMs);
    }

    public boolean shouldRetry(Exception exception, int attemptCount) {
        if (attemptCount >= maxAttempts) {
            return false;
        }
        return retryableExceptionPredicate.test(exception);
    }
}