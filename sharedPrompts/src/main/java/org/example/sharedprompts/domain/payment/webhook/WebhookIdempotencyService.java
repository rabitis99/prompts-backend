package org.example.sharedprompts.domain.payment.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Webhook 멱등성 관리 서비스
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
     * @param webhookId Webhook ID
     * @return 이미 처리 완료된 경우 true
     */
    public boolean isProcessed(String webhookId) {
        String key = LOCK_KEY_PREFIX + webhookId;
        String value = redisTemplate.opsForValue().get(key);
        return LOCK_VALUE_PROCESSED.equals(value);
    }
}
