package org.example.sharedprompts.domain.payment.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 재시도 설정 Properties (Immutable)
 *
 * <p><strong>재시도 정책:</strong>
 * <ul>
 *   <li><strong>즉시 재시도:</strong> 일시적인 네트워크 오류 등에 대해 즉시 재시도 (짧은 횟수)</li>
 *   <li><strong>지연 재시도:</strong> 스케줄러 기반 지수 백오프 재시도 (긴 간격)</li>
 * </ul>
 */
@Getter
@Component
public class RetryProperties {

    /**
     * 최대 재시도 횟수 (지연 재시도 포함)
     */
    private final int maxAttempts;

    /**
     * 지연 재시도 기본 지연 시간 (밀리초)
     */
    private final long delayMs;

    /**
     * 즉시 재시도 최대 횟수
     * 일시적인 네트워크 오류 등에 대해 즉시 재시도
     */
    private final int immediateRetryMaxAttempts;

    /**
     * 즉시 재시도 지연 시간 (밀리초)
     * 각 즉시 재시도 사이의 짧은 지연
     */
    private final long immediateRetryDelayMs;

    public RetryProperties(
            @Value("${payment.retry.max-attempts:3}") int maxAttempts,
            @Value("${payment.retry.delay-ms:1000}") long delayMs,
            @Value("${payment.retry.immediate.max-attempts:2}") int immediateRetryMaxAttempts,
            @Value("${payment.retry.immediate.delay-ms:200}") long immediateRetryDelayMs) {
        this.maxAttempts = maxAttempts;
        this.delayMs = delayMs;
        this.immediateRetryMaxAttempts = immediateRetryMaxAttempts;
        this.immediateRetryDelayMs = immediateRetryDelayMs;
    }
}









