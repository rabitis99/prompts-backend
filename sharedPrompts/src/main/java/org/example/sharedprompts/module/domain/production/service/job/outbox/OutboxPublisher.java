package org.example.sharedprompts.module.domain.production.service.job.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Outbox 테이블의 PENDING/FAILED 행을 주기적으로 읽어 RabbitMQ로 발행한다.
 * production.job.outbox.enabled=true 일 때만 빈 등록.
 * 행 단위 트랜잭션(OutboxRowPublisher)으로 한 행 실패 시에도 이미 발행된 메시지와 커밋된 행은 롤백되지 않는다.
 */
@Component
@ConditionalOnProperty(name = "production.job.outbox.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private static final int MAX_ROWS_PER_POLL = 100;

    private final OutboxRowPublisher rowPublisher;

    @Scheduled(fixedDelayString = "${production.job.outbox.publisher-interval-ms:2000}")
    public void publishPending() {
        for (int i = 0; i < MAX_ROWS_PER_POLL; i++) {
            if (!rowPublisher.publishOne()) {
                break;
            }
        }
    }
}
