package org.example.sharedprompts.domain.payment.service.compensation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 로깅 기반 보상 트랜잭션 큐 구현체
 *
 * <p><strong>현재 구현:</strong>
 * 보상 작업을 로깅만 수행합니다.
 * 향후 Redis, DB 기반 큐로 교체 가능하도록 인터페이스로 추상화되어 있습니다.
 *
 * <p><strong>향후 개선:</strong>
 * - Redis 기반 큐 (RedisQueue)
 * - DB 기반 큐 (DatabaseQueue)
 * - 메시지 큐 기반 (RabbitMQ, Kafka 등)
 *
 * @see CompensationQueue
 */
@Slf4j
@Component
public class LoggingCompensationQueue implements CompensationQueue {

    @Override
    public void enqueue(CompensationTask task) {
        log.error("보상 트랜잭션 작업 큐에 추가: taskType={}, paymentId={}, userId={}, amount={}, failureReason={}, createdAt={}",
                task.taskType(), task.paymentId(), task.userId(), task.amount(), task.failureReason(), task.createdAt());
        
        // TODO: 향후 Redis, DB 기반 큐로 교체 시 여기에 실제 큐 추가 로직 구현
        // 예: redisTemplate.opsForList().rightPush("compensation:queue", task);
        // 예: compensationTaskRepository.save(CompensationTaskEntity.from(task));
    }
}

