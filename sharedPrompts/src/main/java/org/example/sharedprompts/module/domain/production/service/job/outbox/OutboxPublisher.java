package org.example.sharedprompts.module.domain.production.service.job.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.outbox.JobOutboxEntity;
import org.example.sharedprompts.module.domain.production.repository.outbox.JobOutboxRepository;
import org.example.sharedprompts.module.domain.production.service.job.queue.JobQueuePublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Outbox 테이블의 PENDING 행을 주기적으로 읽어 RabbitMQ로 발행한다.
 * production.job.outbox.enabled=true 일 때만 빈 등록.
 */
@Component
@ConditionalOnProperty(name = "production.job.outbox.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final JobOutboxRepository jobOutboxRepository;
    private final JobQueuePublisher jobQueuePublisher;

    @Scheduled(fixedDelayString = "${production.job.outbox.publisher-interval-ms:2000}")
    @Transactional(rollbackFor = Exception.class)
    public void publishPending() {
        List<JobOutboxEntity> pending = jobOutboxRepository.findTop100ByStatusOrderByCreatedAtAsc(JobOutboxEntity.STATUS_PENDING);
        if (pending.isEmpty()) {
            return;
        }
        for (JobOutboxEntity row : pending) {
            try {
                jobQueuePublisher.publishJob(row.getJobId(), row.getMaxRetryCount());
                row.markSent();
                jobOutboxRepository.save(row);
            } catch (Exception e) {
                log.warn("Outbox publish failed - id: {}, jobId: {}, will retry", row.getId(), row.getJobId(), e);
                row.markFailed(e.getMessage());
                jobOutboxRepository.save(row);
            }
        }
    }
}
