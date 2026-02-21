package org.example.sharedprompts.module.domain.production.service.job.lock;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.repository.job.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JobLockManager {
    
    private final JobRepository jobRepository;
    
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 30)
    public Optional<JobEntity> acquireLock(String jobId) {
        JobEntity job = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalStateException("Job not found: " + jobId));
        
        if (job.isFinalState()) {
            return Optional.of(job);
        }
        
        Long currentVersion = job.getVersion();
        int updatedRows = jobRepository.acquireJobLock(jobId, Instant.now(), currentVersion);
        
        if (updatedRows == 0) {
            return Optional.empty();
        }
        
        return jobRepository.findByJobId(jobId);
    }
}

