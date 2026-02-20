package org.example.sharedprompts.module.domain.production.service.job.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;
import org.example.sharedprompts.module.domain.production.repository.job.JobRepository;
import org.example.sharedprompts.module.domain.production.service.job.JobStateService;
import org.example.sharedprompts.module.domain.production.service.job.scheduler.handler.JobRecoveryHandler;
import org.example.sharedprompts.module.domain.production.service.job.scheduler.handler.JobRecoveryHandlerRegistry;
import org.example.sharedprompts.module.domain.production.service.job.scheduler.policy.JobRecoveryProperties;
import org.example.sharedprompts.module.domain.production.service.job.scheduler.policy.StaleThresholdPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerJobRecoveryService {
    
    private final JobRepository jobRepository;
    private final StaleThresholdPolicy staleThresholdPolicy;
    private final JobRecoveryHandlerRegistry handlerRegistry;
    private final JobRecoveryProperties properties;
    private final JobStateService jobStateService;
    
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
                JobRecoveryHandler handler = handlerRegistry.getHandler(job.getStatus());
                handler.recover(job);
            } catch (Exception e) {
                log.error("Failed to recover stale job - jobId: {}", job.getJobId(), e);
                int thresholdMinutes = staleThresholdPolicy.getStaleJobThresholdMinutes();
                jobStateService.saveJobFailure(job.getJobId(), 
                    "Stale job recovery failed: Timeout after " + thresholdMinutes + " minutes");
            }
        }
    }
}

