package org.example.sharedprompts.module.domain.production.service.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.repository.job.JobRepository;
import org.example.sharedprompts.module.domain.production.service.job.metrics.JobMetrics;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.JobProcessingException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobLockService {

    private final JobRepository jobRepository;
    private final JobMetrics jobMetrics;

    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 30)
    public Optional<JobEntity> acquireJobLock(String jobId) {
        try {
            JobEntity job = jobRepository.findByJobId(jobId)
                    .orElseThrow(() -> new JobProcessingException(ModuleErrorCode.JOB_NOT_FOUND, "Job not found: " + jobId));

            if (job.isFinalState()) {
                return Optional.of(job);
            }

            Long currentVersion = job.getVersion();
            int updatedRows = jobRepository.acquireJobLock(jobId, Instant.now(), currentVersion);

            if (updatedRows == 0) {
                log.info("Job lock acquisition failed - jobId: {} (concurrent processing)", jobId);
                return Optional.empty();
            }

            JobEntity lockedJob = jobRepository.findByJobId(jobId)
                    .orElseThrow(() -> new JobProcessingException(
                            ModuleErrorCode.RECOVERY_ERROR,
                            "Job not found after lock acquisition - jobId: " + jobId
                    ));

            log.info("Job lock acquired - jobId: {}, status: {}", jobId, lockedJob.getStatus());
            return Optional.of(lockedJob);
        } catch (TransactionTimedOutException e) {
            log.error("Transaction timeout while acquiring job lock - jobId: {}", jobId, e);
            jobMetrics.recordTransactionTimeout("acquireJobLock");
            throw new JobProcessingException(
                    ModuleErrorCode.RECOVERY_ERROR,
                    "Transaction timeout while acquiring job lock: " + jobId,
                    e
            );
        }
    }
}

