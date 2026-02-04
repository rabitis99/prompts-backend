package org.example.sharedprompts.domain.payment.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Webhook 멱등성 관리 서비스
 *
 * <p>Redis를 사용하여 동시 처리 방지 및 처리 상태 추적을 제공합니다.
 * 이 서비스는 단순히 동시성 제어를 위한 것이며, 진정한 멱등성은 DB 상태를 통해 보장됩니다.
 *
 * <p><strong>DistributedLockService와의 차이점:</strong>
 * 이 서비스는 단순한 락이 아닌 3단계 상태(없음 → processing → processed)를 관리합니다.
 * <ul>
 *   <li>DistributedLockService: 락 획득 → 작업 실행 → 락 해제 (2단계)</li>
 *   <li>WebhookIdempotencyService: 락 획득 → 작업 실행 → 처리 완료 마킹 (3단계, 완료 상태 유지)</li>
 * </ul>
 *
 * <p><strong>설계 원칙:</strong>
 * <ul>
 *   <li>Redis 락 실패가 메인 프로세스를 차단하지 않음</li>
 *   <li>락은 TTL로 자동 만료되어 데드락 방지</li>
 *   <li>예외 발생 시 락이 해제되어 재처리 가능</li>
 *   <li>처리 완료된 Webhook은 7일간 "processed" 상태로 유지되어 중복 처리 방지</li>
 * </ul>
 *
 * <p><strong>권장 사용 패턴:</strong>
 * <pre>{@code
 * if (!idempotencyService.tryAcquireLock(webhookId)) {
 *     return; // 이미 처리 중
 * }
 * try {
 *     processWebhook();
 *     idempotencyService.markAsProcessed(webhookId);
 * } catch (Exception e) {
 *     idempotencyService.releaseLock(webhookId);
 *     throw e;
 * }
 * }</pre>
 *
 * @see org.example.sharedprompts.domain.payment.service.lock.DistributedLockService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookIdempotencyService {

    private static final String LOCK_KEY_PREFIX = "webhook:lock:";
    private static final String LOCK_VALUE_PROCESSING = "processing";
    private static final String LOCK_VALUE_PROCESSED = "processed";
    private static final Duration LOCK_TTL = Duration.ofMinutes(5);
    private static final Duration PROCESSED_TTL = Duration.ofDays(7);

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 처리 락 획득 시도 (원자적 연산)
     *
     * <p>SETNX(SET if Not eXists)를 사용하여 원자적으로 락을 획득합니다.
     * 이미 락이 존재하면 false를 반환합니다.
     *
     * @param webhookId Webhook ID
     * @return 락 획득 성공 시 true, 이미 락이 존재하면 false
     */
    public boolean tryAcquireLock(String webhookId) {
        String key = LOCK_KEY_PREFIX + webhookId;
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(key, LOCK_VALUE_PROCESSING, LOCK_TTL);
        boolean acquired = Boolean.TRUE.equals(result);

        if (acquired) {
            log.debug("Webhook 락 획득: webhookId={}", webhookId);
        } else {
            log.debug("Webhook 락 이미 존재: webhookId={}", webhookId);
        }

        return acquired;
    }

    /**
     * 처리 완료 마킹
     *
     * <p>락 값을 "processed"로 변경하고 TTL을 7일로 연장합니다.
     * 이를 통해 동일 Webhook이 재전송되어도 빠르게 중복 여부를 확인할 수 있습니다.
     *
     * @param webhookId Webhook ID
     */
    public void markAsProcessed(String webhookId) {
        String key = LOCK_KEY_PREFIX + webhookId;
        redisTemplate.opsForValue().set(key, LOCK_VALUE_PROCESSED, PROCESSED_TTL);
        log.debug("Webhook 처리 완료 마킹: webhookId={}", webhookId);
    }

    /**
     * 락 해제 (처리 실패 시)
     *
     * <p>"processing" 상태일 때만 삭제하여, 이미 완료된 것은 보존합니다.
     * 락 해제 실패 시에도 TTL로 자동 만료됩니다.
     *
     * @param webhookId Webhook ID
     */
    public void releaseLock(String webhookId) {
        String key = LOCK_KEY_PREFIX + webhookId;
        String value = redisTemplate.opsForValue().get(key);

        if (LOCK_VALUE_PROCESSING.equals(value)) {
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("Webhook 락 해제: webhookId={}", webhookId);
            }
        }
    }

    /**
     * Webhook이 이미 처리 완료 상태인지 확인
     *
     * <p>Redis에서 빠르게 중복 여부를 확인할 수 있지만,
     * 최종 멱등성은 DB 상태를 통해 보장됩니다.
     *
     * @param webhookId Webhook ID
     * @return 이미 처리 완료된 경우 true
     */
    public boolean isProcessed(String webhookId) {
        String key = LOCK_KEY_PREFIX + webhookId;
        String value = redisTemplate.opsForValue().get(key);
        return LOCK_VALUE_PROCESSED.equals(value);
    }
}
