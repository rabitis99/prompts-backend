package org.example.sharedprompts.module.domain.production.service.ai.retry;

/**
 * 재시도 정책 인터페이스
 */
public interface RetryPolicy {
    
    /**
     * 최대 재시도 횟수
     */
    int getMaxRetries();
    
    /**
     * 재시도 가능 여부를 판단하는 조건
     * 
     * @param attempt 현재 시도 횟수 (0-based: 0 = 첫 번째 실패 후 첫 번째 재시도 전)
     * @param throwable 발생한 예외
     * @return 재시도 가능 여부
     */
    boolean shouldRetry(int attempt, Throwable throwable);
    
    /**
     * 재시도 전 대기 시간 계산 (밀리초)
     * 
     * @param attempt 현재 시도 횟수 (0-based: 0 = 첫 번째 실패 후 첫 번째 재시도 전, shouldRetry와 동일한 기준)
     * @return 대기 시간 (밀리초)
     */
    long calculateDelayMs(int attempt);
}

