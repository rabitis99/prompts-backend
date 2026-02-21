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

    private static final List<String> PUBLISH_STATUSES = List.of(
            JobOutboxEntity.STATUS_PENDING,
            JobOutboxEntity.STATUS_FAILED
    );

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
            log.warn("Outbox publish failed - id: {}, jobId: {}, will retry on next poll", row.getId(), row.getJobId(), e);
            row.markFailed(e.getMessage());
            jobOutboxRepository.save(row);
        }
        return true;
    }
}
