package org.example.sharedprompts.module.domain.production.service.job.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.outbox.JobOutboxEntity;
import org.example.sharedprompts.module.domain.production.repository.outbox.JobOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Job 생성과 동일 트랜잭션에서 Outbox 행을 기록한다.
 * OutboxPublisher가 주기적으로 PENDING을 읽어 RabbitMQ로 발행한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobOutboxService {

    private final JobOutboxRepository jobOutboxRepository;

    @Transactional(rollbackFor = Exception.class)
    public void enqueue(String jobId, int maxRetryCount) {
        JobOutboxEntity entity = JobOutboxEntity.builder()
                .jobId(jobId)
                .maxRetryCount(maxRetryCount)
                .status(JobOutboxEntity.STATUS_PENDING)
                .build();
        jobOutboxRepository.save(entity);
        log.debug("Outbox enqueued - jobId: {}, maxRetryCount: {}", jobId, maxRetryCount);
    }
}
