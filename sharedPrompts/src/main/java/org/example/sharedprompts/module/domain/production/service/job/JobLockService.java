package org.example.sharedprompts.module.domain.production.service.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.service.job.lock.JobLockManager;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobLockService {

    private final JobLockManager lockManager;

    public Optional<JobEntity> acquireJobLock(String jobId) {
        Optional<JobEntity> result = lockManager.acquireLock(jobId);
        if (result.isPresent()) {
            log.info("Job lock acquired - jobId: {}, status: {}", jobId, result.get().getStatus());
        } else {
            log.info("Job lock acquisition failed - jobId: {} (concurrent processing)", jobId);
        }
        return result;
    }
}

