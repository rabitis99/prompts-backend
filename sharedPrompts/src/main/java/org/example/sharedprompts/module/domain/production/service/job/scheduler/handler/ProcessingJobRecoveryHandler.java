package org.example.sharedprompts.module.domain.production.service.job.scheduler.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;
import org.example.sharedprompts.module.domain.production.service.job.JobStateService;
import org.example.sharedprompts.module.domain.production.service.job.queue.JobQueuePublisher;
import org.springframework.stereotype.Component;

/**
 * Stuck PROCESSING 복구: processJob()을 호출하면 acquireJobLock()이 PENDING/RETRYING만 허용해
 * PROCESSING은 락 획득 불가이므로, 직접 PROCESSING→FAILED→PENDING 전이 후 재큐한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessingJobRecoveryHandler implements JobRecoveryHandler {

    private static final int DEFAULT_MAX_RETRY_COUNT = 3;

    private final JobStateService jobStateService;
    private final JobQueuePublisher jobQueuePublisher;

    @Override
    public boolean supports(JobStatus status) {
        return status == JobStatus.PROCESSING;
    }

    @Override
    public void recover(JobEntity job) {
        String jobId = job.getJobId();
        log.info("Recovering stuck PROCESSING job - jobId: {}, retryCount: {}",
                jobId, job.getRetryCount());

        jobStateService.saveJobFailure(jobId,
                "Stale PROCESSING job recovered (stuck beyond threshold)");
        jobStateService.retryJob(jobId);
        jobQueuePublisher.publishJob(jobId, DEFAULT_MAX_RETRY_COUNT);
    }
}

