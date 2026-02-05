package org.example.sharedprompts.domain.payment.service.retry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.RetryProperties;
import org.example.sharedprompts.global.exception.ApiException;
import org.springframework.stereotype.Component;

/**
 * 즉시 재시도 전략 구현체
 *
 * <p><strong>특징:</strong>
 * <ul>
 *   <li>일시적인 네트워크 오류 등에 대해 즉시 재시도</li>
 *   <li>짧은 고정 지연 시간 사용</li>
 *   <li>적은 최대 재시도 횟수 (기본 2회)</li>
 * </ul>
 *
 * <p><strong>재시도 대상:</strong>
 * <ul>
 *   <li>네트워크 오류 (timeout, connection, network, unavailable)</li>
 *   <li>일시적인 오류 (temporary, retry)</li>
 * </ul>
 *
 * <p><strong>재시도 비대상:</strong>
 * <ul>
 *   <li>비즈니스 로직 오류 (잔액 부족, 카드 한도 초과 등)</li>
 *   <li>인증 오류</li>
 * </ul>
 *
 * @see RetryStrategy
 * @see org.example.sharedprompts.domain.payment.service.execution.PaymentExecutionService
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImmediateRetryStrategy implements RetryStrategy {

    private final RetryProperties retryProperties;

    @Override
    public boolean shouldRetry(Exception exception, int attemptCount) {
        // 최대 횟수 초과 시 재시도 불가
        if (attemptCount >= getMaxAttempts()) {
            return false;
        }

        // ApiException인 경우 메시지 기반 판단
        if (exception instanceof ApiException) {
            return isRetryableError((ApiException) exception);
        }

        // RuntimeException인 경우 메시지 기반 판단
        String message = exception.getMessage() != null ? exception.getMessage().toLowerCase() : "";
        return message.contains("timeout") || message.contains("connection") ||
                message.contains("network") || message.contains("unavailable") ||
                message.contains("temporary") || message.contains("retry");
    }

    @Override
    public long calculateDelay(int attemptCount) {
        // 즉시 재시도는 고정 지연 시간 사용
        return retryProperties.getImmediateRetryDelayMs();
    }

    @Override
    public int getMaxAttempts() {
        return retryProperties.getImmediateRetryMaxAttempts();
    }

    /**
     * 재시도 가능한 오류인지 확인
     *
     * <p>향후 개선: ErrorCode에 isRetryable() 메서드를 추가하여 재시도 가능 여부를 명시적으로 관리하는 것을 권장합니다.
     *
     * @param e ApiException
     * @return 재시도 가능 여부
     */
    private boolean isRetryableError(ApiException e) {
        // ErrorCode 기반 판단 (향후 개선)
        // if (e.getErrorCode() != null && e.getErrorCode().isRetryable()) {
        //     return true;
        // }

        // Fallback: 메시지 기반 판단
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

        // 네트워크 오류, 타임아웃 등은 재시도 가능
        if (message.contains("timeout") || message.contains("connection") ||
                message.contains("network") || message.contains("unavailable") ||
                message.contains("temporary") || message.contains("retry")) {
            return true;
        }

        // 특정 에러 코드는 재시도 불가
        // 비즈니스 로직 오류는 재시도 불가
        return false;
    }
}

