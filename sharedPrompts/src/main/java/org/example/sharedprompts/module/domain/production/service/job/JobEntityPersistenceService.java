package org.example.sharedprompts.module.domain.production.service.job;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.repository.job.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Isolates job save in a new transaction so that constraint violations (e.g. duplicate
 * idempotency key) only abort this transaction. Ensures compatibility with PostgreSQL,
 * which aborts the entire transaction on constraint violation; the caller can then
 * run findByIdempotencyKeyForUpdate in its own transaction.
 */
@Service
@RequiredArgsConstructor
public class JobEntityPersistenceService {

    private final JobRepository jobRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobEntity saveInNewTransaction(JobEntity job) {
        return jobRepository.save(job);
    }
}
