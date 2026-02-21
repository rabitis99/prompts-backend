package org.example.sharedprompts.module.domain.production.service.job.scheduler.policy;

import lombok.Getter;
import lombok.Setter;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "job.recovery")
public class JobRecoveryProperties {
    
    private int maxRetryCount = 3;
    private int staleJobThresholdMinutes = 30;
    
    private List<JobStatus> recoverableStatuses = List.of(
        JobStatus.PENDING,
        JobStatus.PROCESSING,
        JobStatus.UNKNOWN   // P3-1: timeout ambiguity 상태 - threshold 초과 시 FAILED로 전이
    );
}

