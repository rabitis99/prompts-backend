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
     */
    boolean shouldRetry(int attempt, Throwable throwable);
    
    /**
     * 재시도 전 대기 시간 계산 (밀리초)
     */
    long calculateDelayMs(int attempt);
}

