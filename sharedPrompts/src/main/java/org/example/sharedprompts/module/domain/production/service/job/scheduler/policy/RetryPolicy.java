package org.example.sharedprompts.module.domain.production.service.job.scheduler.policy;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.springframework.stereotype.Component;

/**
 * 재시도 횟수 정책 관리
 * 단일 책임: 재시도 횟수 관련 정책만 담당
 */
@Component
@RequiredArgsConstructor
public class RetryPolicy {
    
    private final JobRecoveryProperties properties;
    
    /**
     * 최대 재시도 횟수를 초과했는지 확인
     */
    public boolean isMaxRetryExceeded(JobEntity job) {
        return job.getRetryCount() >= properties.getMaxRetryCount();
    }
    
    /**
     * 최대 재시도 횟수 반환
     */
    public int getMaxRetryCount() {
        return properties.getMaxRetryCount();
    }
}

