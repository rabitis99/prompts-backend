package org.example.sharedprompts.module.domain.production.service.job.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.repository.job.JobRepository;
import org.example.sharedprompts.module.domain.production.service.job.metrics.JobMetrics;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.JobProcessingException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Consumer;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobUpdateTransactionService {

    private final JobRepository jobRepository;
    private final TransactionTemplate transactionTemplate;
    private final JobMetrics jobMetrics;

    public JobUpdateTransactionService(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            JobMetrics jobMetrics) {
        this.jobRepository = jobRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.transactionTemplate.setTimeout(30);
        this.jobMetrics = jobMetrics;
    }

    public void updateJobInTransaction(String jobId, Consumer<JobEntity> updater) {
        try {
            transactionTemplate.execute(status -> {
                JobEntity job = jobRepository.findByJobId(jobId)
                        .orElseThrow(() -> new JobProcessingException(ModuleErrorCode.JOB_NOT_FOUND, "Job not found: " + jobId));
                updater.accept(job);
                jobRepository.save(job);
                return null;
            });
        } catch (TransactionTimedOutException e) {
            log.error("Transaction timeout - jobId: {}", jobId, e);
            jobMetrics.recordTransactionTimeout("updateJobInTransaction");
            throw new JobProcessingException(
                    ModuleErrorCode.RECOVERY_ERROR,
                    "Transaction timeout while updating job: " + jobId,
                    e
            );
        }
    }

    public <T> T updateJobAndReturnInTransaction(String jobId, Function<JobEntity, T> updater) {
        try {
            return transactionTemplate.execute(status -> {
                JobEntity job = jobRepository.findByJobId(jobId)
                        .orElseThrow(() -> new JobProcessingException(ModuleErrorCode.JOB_NOT_FOUND, "Job not found: " + jobId));
                T result = updater.apply(job);
                jobRepository.save(job);
                return result;
            });
        } catch (TransactionTimedOutException e) {
            log.error("Transaction timeout - jobId: {}", jobId, e);
            jobMetrics.recordTransactionTimeout("updateJobAndReturnInTransaction");
            throw new JobProcessingException(
                    ModuleErrorCode.RECOVERY_ERROR,
                    "Transaction timeout while updating job: " + jobId,
                    e
            );
        }
    }
}

