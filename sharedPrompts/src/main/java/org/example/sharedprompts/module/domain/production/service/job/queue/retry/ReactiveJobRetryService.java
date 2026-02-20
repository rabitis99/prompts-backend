package org.example.sharedprompts.module.domain.production.service.job.queue.retry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.service.job.queue.JobQueuePublisher;
import org.example.sharedprompts.module.domain.production.service.job.queue.message.JobMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReactiveJobRetryService {

    private final JobQueuePublisher jobQueuePublisher;

    private static final long INITIAL_RETRY_DELAY_MS = 500;
    private static final long MAX_RETRY_DELAY_MS = 30000;

    public Mono<Void> scheduleRetry(JobMessage message) {
        if (message.isMaxRetryExceeded()) {
            log.warn("Max retry count exceeded - jobId: {}, retryCount: {}/{}",
                    message.getJobId(),
                    message.getRetryCount(),
                    message.getMaxRetryCount());
            return Mono.empty();
        }

        long delayMs = calculateBackoffDelay(message.getRetryCount());
        
        log.info("Scheduling job retry - jobId: {}, retryCount: {}/{}, delay: {}ms",
                message.getJobId(),
                message.getRetryCount() + 1,
                message.getMaxRetryCount(),
                delayMs);

        return Mono.delay(Duration.ofMillis(delayMs))
                .then(Mono.fromRunnable(() -> {
                    JobMessage retryMessage = message.withIncrementedRetry();
                    jobQueuePublisher.publishRetry(retryMessage);
                }))
                .then();
    }

    private long calculateBackoffDelay(int retryCount) {
        long delayMs = INITIAL_RETRY_DELAY_MS * (1L << retryCount);
        return Math.min(delayMs, MAX_RETRY_DELAY_MS);
    }

    public static Retry createRetryBackoff(int maxRetries) {
        return Retry.backoff(maxRetries, Duration.ofMillis(INITIAL_RETRY_DELAY_MS))
                .maxBackoff(Duration.ofMillis(MAX_RETRY_DELAY_MS));
    }
}

