package org.example.sharedprompts.domain.payment.infrastructure.retry;

/**
 * 재시도 전략 인터페이스
 */
public interface RetryStrategy {

    /**
     * 재시도 가능한 오류인지 확인합니다.
     * @param exception 발생한 예외
     * @param attemptCount 현재 시도 횟수 (0부터 시작)
     * @return 재시도 가능 여부
     */
    boolean shouldRetry(Exception exception, int attemptCount);

    /**
     * 재시도 지연 시간을 계산합니다.
     * @param attemptCount 현재 시도 횟수 (0부터 시작)
     * @return 재시도 지연 시간 (밀리초)
     */
    long calculateDelay(int attemptCount);

    /**
     * 최대 재시도 횟수를 반환합니다.
     * @return 최대 재시도 횟수
     */
    int getMaxAttempts();
}

