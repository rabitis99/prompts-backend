package org.example.sharedprompts.domain.payment.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 재시도 설정 Properties (Immutable)
 */
@Getter
@Component
public class RetryProperties {

    private final int maxAttempts;
    private final long delayMs;

    public RetryProperties(
            @Value("${payment.retry.max-attempts:3}") int maxAttempts,
            @Value("${payment.retry.delay-ms:1000}") long delayMs) {
        this.maxAttempts = maxAttempts;
        this.delayMs = delayMs;
    }
}




