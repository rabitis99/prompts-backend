package org.example.sharedprompts.module.domain.production.service.job.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;
import org.example.sharedprompts.module.domain.production.repository.job.JobRepository;
import org.example.sharedprompts.module.domain.production.service.job.scheduler.handler.JobRecoveryHandler;
import org.example.sharedprompts.module.domain.production.service.job.scheduler.handler.JobRecoveryHandlerRegistry;
import org.example.sharedprompts.module.domain.production.service.job.scheduler.policy.JobRecoveryProperties;
import org.example.sharedprompts.module.domain.production.service.job.scheduler.policy.RetryPolicy;
import org.example.sharedprompts.module.domain.production.service.job.scheduler.policy.StaleThresholdPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Job 복구 서비스
 * 단일 책임: Job 조회 + 상태별 Handler 위임
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerJobRecoveryService {
    
    private final JobRepository jobRepository;
    private final RetryPolicy retryPolicy;
    private final StaleThresholdPolicy staleThresholdPolicy;
    private final JobRecoveryHandlerRegistry handlerRegistry;
    private final JobRecoveryProperties properties;
    
    /**
     * 만료된 Job들을 복구
     */
    @Transactional
    public void recoverStaleJobs() {
        Instant threshold = staleThresholdPolicy.calculateThreshold();
        List<JobStatus> recoverableStatuses = properties.getRecoverableStatuses();
        
        List<JobEntity> staleJobs = jobRepository.findStuckJobsInStatuses(
            recoverableStatuses, threshold
        );
        
        if (staleJobs.isEmpty()) {
            return;
        }
        
        log.info("Found {} stale jobs to recover", staleJobs.size());
        
        for (JobEntity job : staleJobs) {
            try {
                if (retryPolicy.isMaxRetryExceeded(job)) {
                    handleMaxRetryExceeded(job);
                    continue;
                }
                
                JobRecoveryHandler handler = handlerRegistry.getHandler(job.getStatus());
                handler.recover(job);
            } catch (Exception e) {
                log.error("Failed to recover stale job - jobId: {}", job.getJobId(), e);
            }
        }
    }
    
    /**
     * 최대 재시도 횟수를 초과한 Job을 실패 처리
     */
    private void handleMaxRetryExceeded(JobEntity job) {
        int thresholdMinutes = staleThresholdPolicy.getStaleJobThresholdMinutes();
        job.fail("Timeout after " + thresholdMinutes + " minutes");
        jobRepository.save(job);
        
        log.warn("Marked stale job as failed - jobId: {}, retryCount: {}",
            job.getJobId(), job.getRetryCount());
    }
}

