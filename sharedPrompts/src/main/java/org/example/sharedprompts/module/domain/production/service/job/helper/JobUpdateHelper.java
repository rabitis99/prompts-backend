package org.example.sharedprompts.module.domain.production.service.job.helper;

import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.service.job.metrics.JobMetrics;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.JobProcessingException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobUpdateHelper {

    private static final int MAX_RETRIES = 3;
    private static final int INITIAL_RETRY_DELAY_MS = 10;

    private final JobUpdateTransactionService transactionService;
    private final JobMetrics jobMetrics;

    public void updateJob(String jobId, Consumer<JobEntity> updater) {
        Timer.Sample sample = jobMetrics.startUpdateTimer();
        boolean success = false;
        
        try {
            for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
                try {
                    transactionService.updateJobInTransaction(jobId, updater);
                    success = true;
                    return;
                } catch (OptimisticLockingFailureException e) {
                    if (attempt == MAX_RETRIES - 1) {
                        log.error("Failed to update job after {} retries - jobId: {}", MAX_RETRIES, jobId, e);
                        jobMetrics.recordOptimisticLockFailure(jobId);
                        throw new JobProcessingException(
                                ModuleErrorCode.RECOVERY_ERROR,
                                String.format("Failed to update job after %d retries due to optimistic lock conflict - jobId: %s", MAX_RETRIES, jobId),
                                e
                        );
                    }
                    log.warn("Optimistic lock conflict, retrying - jobId: {}, attempt: {}/{}", jobId, attempt + 1, MAX_RETRIES);
                    jobMetrics.recordOptimisticLockRetry(jobId, attempt + 1);
                    try {
                        Thread.sleep(INITIAL_RETRY_DELAY_MS * (attempt + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new JobProcessingException(
                                ModuleErrorCode.RECOVERY_ERROR,
                                "Interrupted during retry - jobId: " + jobId,
                                ie
                        );
                    }
                }
            }
        } finally {
            jobMetrics.recordUpdateDuration(sample, "updateJob", success);
        }
    }

    public <T> T updateJobAndReturn(String jobId, Function<JobEntity, T> updater) {
        Timer.Sample sample = jobMetrics.startUpdateTimer();
        boolean success = false;
        
        try {
            for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
                try {
                    T result = transactionService.updateJobAndReturnInTransaction(jobId, updater);
                    success = true;
                    return result;
                } catch (OptimisticLockingFailureException e) {
                    if (attempt == MAX_RETRIES - 1) {
                        log.error("Failed to update job after {} retries - jobId: {}", MAX_RETRIES, jobId, e);
                        jobMetrics.recordOptimisticLockFailure(jobId);
                        throw new JobProcessingException(
                                ModuleErrorCode.RECOVERY_ERROR,
                                String.format("Failed to update job after %d retries due to optimistic lock conflict - jobId: %s", MAX_RETRIES, jobId),
                                e
                        );
                    }
                    log.warn("Optimistic lock conflict, retrying - jobId: {}, attempt: {}/{}", jobId, attempt + 1, MAX_RETRIES);
                    jobMetrics.recordOptimisticLockRetry(jobId, attempt + 1);
                    try {
                        Thread.sleep(INITIAL_RETRY_DELAY_MS * (attempt + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new JobProcessingException(
                                ModuleErrorCode.RECOVERY_ERROR,
                                "Interrupted during retry - jobId: " + jobId,
                                ie
                        );
                    }
                }
            }
            throw new JobProcessingException(
                    ModuleErrorCode.RECOVERY_ERROR,
                    "Unexpected error: retry loop completed without success - jobId: " + jobId
            );
        } finally {
            jobMetrics.recordUpdateDuration(sample, "updateJobAndReturn", success);
        }
    }
}

