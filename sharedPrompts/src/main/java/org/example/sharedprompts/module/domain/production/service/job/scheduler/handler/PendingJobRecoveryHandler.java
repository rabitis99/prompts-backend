package org.example.sharedprompts.module.domain.production.service.job.scheduler.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;
import org.example.sharedprompts.module.domain.production.service.job.queue.JobQueuePublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PendingJobRecoveryHandler implements JobRecoveryHandler {
    
    private final JobQueuePublisher jobQueuePublisher;
    
    @Override
    public boolean supports(JobStatus status) {
        return status == JobStatus.PENDING;
    }
    
    @Override
    public void recover(JobEntity job) {
        log.info("Retrying PENDING job - jobId: {}, retryCount: {}", 
            job.getJobId(), job.getRetryCount());
        
        jobQueuePublisher.publishJob(job.getJobId(), 3);
    }
}

