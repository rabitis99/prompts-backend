package org.example.sharedprompts.module.domain.production.service.job.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.outbox.JobOutboxEntity;
import org.example.sharedprompts.module.domain.production.repository.outbox.JobOutboxRepository;
import org.example.sharedprompts.module.domain.production.service.job.queue.JobQueuePublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Single outbox row publish in its own transaction: find with FOR UPDATE SKIP LOCKED,
 * then publish and save in the same transaction so the lock is held until commit.
 * Prevents duplicate RabbitMQ publish and allows per-row rollback on failure.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRowPublisher {

    /** PENDING만 재발행 대상. FAILED는 maxRetryCount 초과 후 최종 실패로 두어 무한 재시도 방지. */
    private static final List<String> PUBLISH_STATUSES = List.of(JobOutboxEntity.STATUS_PENDING);

    private final JobOutboxRepository jobOutboxRepository;
    private final JobQueuePublisher jobQueuePublisher;

    /**
     * Finds one row with lock, publishes to RabbitMQ, updates row. All in one transaction.
     *
     * @return true if a row was processed, false if none available
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean publishOne() {
        List<JobOutboxEntity> batch = jobOutboxRepository.findTop1ByStatusInOrderByCreatedAtAscForUpdate(PUBLISH_STATUSES);
        if (batch.isEmpty()) {
            return false;
        }
        JobOutboxEntity row = batch.get(0);
        try {
            jobQueuePublisher.publishJob(row.getJobId(), row.getMaxRetryCount());
            row.markSent();
            jobOutboxRepository.save(row);
        } catch (Exception e) {
            row.incrementRetryCount();
            String message = e.getMessage() != null && e.getMessage().length() > 500
                    ? e.getMessage().substring(0, 500) : e.getMessage();
            if (row.getRetryCount() >= row.getMaxRetryCount()) {
                log.warn("Outbox publish failed after max retries - id: {}, jobId: {}, giving up", row.getId(), row.getJobId(), e);
                row.markFailed(message);
            } else {
                log.warn("Outbox publish failed - id: {}, jobId: {}, retryCount: {}/{}, will retry on next poll",
                        row.getId(), row.getJobId(), row.getRetryCount(), row.getMaxRetryCount(), e);
                row.recordRetryFailure(message);
            }
            try {
                jobOutboxRepository.save(row);
            } catch (Exception saveEx) {
                log.error("Failed to persist outbox row failure state - id: {}, jobId: {}; row will be retried as PENDING",
                        row.getId(), row.getJobId(), saveEx);
                // 트랜잭션이 롤백되어 행은 PENDING 상태로 복원됨
            }
        }
        return true;
    }
}
