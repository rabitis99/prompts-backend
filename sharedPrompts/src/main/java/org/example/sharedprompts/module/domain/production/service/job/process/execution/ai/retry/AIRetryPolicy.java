package org.example.sharedprompts.module.domain.production.service.job.process.execution.ai.retry;

import org.springframework.stereotype.Component;

/**
 * AI 재시도 backoff 정책
 *
 * [P0-2 현황]
 * sleep() 메서드는 consumer 스레드를 blocking 함.
 * ScheduledExecutorService + CompletableFuture.get() 조합을 Thread.sleep()으로 단순화.
 * 동작은 동일하나 불필요한 ScheduledExecutorService 의존성 제거.
 * 완전한 non-blocking 전환은 P2-1에서 reactive pipeline 전환 시 수행.
 */
@Component
public class AIRetryPolicy {

    private static final long MAX_BACKOFF_MS = 3000;
    private static final long INITIAL_BACKOFF_MS = 500;

    public long calculateBackoff(int retryCount) {
        long backoffMs = (long) (INITIAL_BACKOFF_MS * Math.pow(2, retryCount - 1));
        return Math.min(backoffMs, MAX_BACKOFF_MS);
    }

    /**
     * [P0-2] Consumer 스레드 blocking 발생 지점
     * 완전 제거는 P2-1 reactive pipeline 전환 시 수행
     */
    public void sleep(long backoffMs) {
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
