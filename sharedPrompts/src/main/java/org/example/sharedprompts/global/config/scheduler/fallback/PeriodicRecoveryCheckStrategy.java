package org.example.sharedprompts.global.config.scheduler.fallback;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 주기적으로 Redis 복구를 체크하는 전략
 * N번 중 1번만 Redis를 시도하여 성능 오버헤드를 최소화
 */
@Slf4j
public class PeriodicRecoveryCheckStrategy implements RecoveryCheckStrategy {
    
    private final AtomicLong counter = new AtomicLong(0);
    private final long checkInterval;
    
    /**
     * @param checkInterval N번 중 1번 체크 (예: 10이면 10번 중 1번)
     */
    public PeriodicRecoveryCheckStrategy(long checkInterval) {
        if (checkInterval <= 0) {
            throw new IllegalArgumentException("checkInterval must be positive");
        }
        this.checkInterval = checkInterval;
    }
    
    @Override
    public boolean shouldCheck() {
        long current = counter.incrementAndGet();
        boolean shouldCheck = (current % checkInterval == 0);
        
        if (shouldCheck) {
            log.debug("Recovery check triggered (check #{})", current);
        }
        
        return shouldCheck;
    }
    
    @Override
    public void onCheckCompleted() {
        // 주기적 체크 전략에서는 별도 처리 불필요
    }
    
    @Override
    public void reset() {
        counter.set(0);
        log.debug("Recovery check counter reset");
    }
    
    /**
     * 현재 카운터 값 (모니터링용)
     */
    public long getCurrentCount() {
        return counter.get();
    }
}

