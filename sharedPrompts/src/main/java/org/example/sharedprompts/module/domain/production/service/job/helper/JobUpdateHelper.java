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

    private static final int MAX_ATTEMPTS = 3;
    private static final int INITIAL_RETRY_DELAY_MS = 10;

    private final JobUpdateTransactionService transactionService;
    private final JobMetrics jobMetrics;

    public void updateJob(String jobId, Consumer<JobEntity> updater) {
        executeWithRetry(
                jobId,
                "updateJob",
                () -> {
                    transactionService.updateJobInTransaction(jobId, updater);
                    return null;
                }
        );
    }

    public <T> T updateJobAndReturn(String jobId, Function<JobEntity, T> updater) {
        return executeWithRetry(
                jobId,
                "updateJobAndReturn",
                () -> transactionService.updateJobAndReturnInTransaction(jobId, updater)
        );
    }

    /**
     * 공통 재시도 로직: OptimisticLockingFailureException 발생 시 지수 백오프로 재시도
     * 최대 대기 시간: 10ms + 20ms = 30ms (3회, 매우 짧음)
     */
    private <T> T executeWithRetry(String jobId, String operationName, RetryableOperation<T> operation) {
        Timer.Sample sample = jobMetrics.startUpdateTimer();
        boolean success = false;

        try {
            for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
                try {
                    T result = operation.execute();
                    success = true;
                    return result;
                } catch (OptimisticLockingFailureException e) {
                    if (attempt == MAX_ATTEMPTS - 1) {
                        int retryCount = MAX_ATTEMPTS - 1;
                        log.error("Failed to update job after {} retries - jobId: {}", retryCount, jobId, e);
                        jobMetrics.recordOptimisticLockFailure();
                        throw new JobProcessingException(
                                ModuleErrorCode.RECOVERY_ERROR,
                                String.format("Failed to update job after %d retries due to optimistic lock conflict - jobId: %s", retryCount, jobId),
                                e
                        );
                    }
                    log.warn("Optimistic lock conflict, retrying - jobId: {}, attempt: {}/{}", jobId, attempt + 1, MAX_ATTEMPTS);
                    sleepWithExponentialBackoff(attempt, jobId);
                    jobMetrics.recordOptimisticLockRetry(attempt + 1);
                }
            }
            // 컴파일러 요구사항: MAX_ATTEMPTS > 0인 경우 이 지점에는 도달하지 않습니다.
            throw new JobProcessingException(
                    ModuleErrorCode.RECOVERY_ERROR,
                    "Unexpected error: retry loop completed without success - jobId: " + jobId
            );
        } finally {
            jobMetrics.recordUpdateDuration(sample, operationName, success);
        }
    }

    private void sleepWithExponentialBackoff(int attempt, String jobId) {
        long delayMs = INITIAL_RETRY_DELAY_MS * (1L << attempt);
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new JobProcessingException(
                    ModuleErrorCode.RECOVERY_ERROR,
                    "Interrupted during retry - jobId: " + jobId,
                    ie
            );
        }
    }

    @FunctionalInterface
    private interface RetryableOperation<T> {
        T execute();
    }
}
