package org.example.sharedprompts.domain.admin.maintenance.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 재빌드 작업 메트릭 관리
 */
@Component
@RequiredArgsConstructor
public class RebuildMetrics {
    
    private final MeterRegistry meterRegistry;
    
    private Counter successCounter;
    private Counter failureCounter;
    private Counter shedLockFailureCounter;
    private Timer durationTimer;
    
    /**
     * 메트릭 초기화 (지연 초기화)
     */
    public void init() {
        if (successCounter == null) {
            successCounter = Counter.builder("admin.maintenance.rebuild.success")
                    .description("좋아요 카운트 재빌드 성공 횟수")
                    .register(meterRegistry);
            
            failureCounter = Counter.builder("admin.maintenance.rebuild.failure")
                    .description("좋아요 카운트 재빌드 실패 횟수")
                    .tag("type", "rebuild")
                    .register(meterRegistry);
            
            shedLockFailureCounter = Counter.builder("admin.maintenance.rebuild.shedlock.failure")
                    .description("ShedLock 획득 실패 횟수")
                    .register(meterRegistry);
            
            durationTimer = Timer.builder("admin.maintenance.rebuild.duration")
                    .description("좋아요 카운트 재빌드 소요 시간")
                    .register(meterRegistry);
        }
    }
    
    public Counter getSuccessCounter() {
        return successCounter;
    }
    
    public Counter getFailureCounter() {
        return failureCounter;
    }
    
    public Counter getShedLockFailureCounter() {
        return shedLockFailureCounter;
    }
    
    public Timer getDurationTimer() {
        return durationTimer;
    }
}

