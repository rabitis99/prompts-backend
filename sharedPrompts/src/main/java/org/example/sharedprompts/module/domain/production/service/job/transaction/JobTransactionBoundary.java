package org.example.sharedprompts.module.domain.production.service.job.transaction;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.repository.job.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Consumer;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JobTransactionBoundary {
    
    private final JobRepository jobRepository;
    private final TransactionTemplate transactionTemplate;
    
    public JobTransactionBoundary(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.transactionTemplate.setTimeout(30);
    }
    
    public void executeInTransaction(String jobId, Consumer<JobEntity> operation) {
        transactionTemplate.execute(status -> {
            JobEntity job = jobRepository.findByJobId(jobId)
                    .orElseThrow(() -> new IllegalStateException("Job not found: " + jobId));
            operation.accept(job);
            jobRepository.save(job);
            return null;
        });
    }
    
    public <T> T executeInTransaction(String jobId, Function<JobEntity, T> operation) {
        return transactionTemplate.execute(status -> {
            JobEntity job = jobRepository.findByJobId(jobId)
                    .orElseThrow(() -> new IllegalStateException("Job not found: " + jobId));
            T result = operation.apply(job);
            jobRepository.save(job);
            return result;
        });
    }
}

