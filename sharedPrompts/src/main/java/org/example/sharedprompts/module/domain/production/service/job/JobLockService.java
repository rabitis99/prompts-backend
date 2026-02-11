package org.example.sharedprompts.module.domain.production.service.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.repository.job.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobLockService {

    private final JobRepository jobRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<JobEntity> acquireJobLock(String jobId) {
        JobEntity job = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        if (job.isFinalState()) {
            return Optional.of(job);
        }

        Long currentVersion = job.getVersion();
        int updatedRows = jobRepository.acquireJobLock(jobId, Instant.now(), currentVersion);

        if (updatedRows == 0) {
            log.info("Job lock acquisition failed - jobId: {} (concurrent processing)", jobId);
            return Optional.empty();
        }

        JobEntity lockedJob = jobRepository.findLockedJob(jobId)
                .orElseThrow(() -> new IllegalStateException("Job not found after lock: " + jobId));

        log.info("Job lock acquired - jobId: {}, status: {}", jobId, lockedJob.getStatus());
        return Optional.of(lockedJob);
    }
}

