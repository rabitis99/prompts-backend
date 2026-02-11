package org.example.sharedprompts.module.domain.production.service.job.scheduler.policy;

import lombok.Getter;
import lombok.Setter;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Job 복구 관련 설정 Properties
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "job.recovery")
public class JobRecoveryProperties {
    
    /**
     * 최대 재시도 횟수
     */
    private int maxRetryCount = 3;
    
    /**
     * Job 만료 기준 시간 (분)
     */
    private int staleJobThresholdMinutes = 30;
    
    /**
     * 복구 가능한 Job 상태 목록
     */
    private List<JobStatus> recoverableStatuses = List.of(
        JobStatus.PENDING,
        JobStatus.PROCESSING,
        JobStatus.AI_CALLED,
        JobStatus.PARSED,
        JobStatus.RENDERED,
        JobStatus.STORED
    );
}

