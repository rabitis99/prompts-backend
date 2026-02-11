package org.example.sharedprompts.module.domain.production.service.job.scheduler.policy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Job 만료 기준 정책 관리
 * 단일 책임: Job 만료 기준 시간 계산만 담당
 */
@Component
@RequiredArgsConstructor
public class StaleThresholdPolicy {
    
    private final JobRecoveryProperties properties;
    
    /**
     * 만료 기준 시간 계산
     */
    public Instant calculateThreshold() {
        return Instant.now().minus(
            properties.getStaleJobThresholdMinutes(), 
            ChronoUnit.MINUTES
        );
    }
    
    /**
     * Job 만료 기준 시간 (분) 반환
     */
    public int getStaleJobThresholdMinutes() {
        return properties.getStaleJobThresholdMinutes();
    }
}

