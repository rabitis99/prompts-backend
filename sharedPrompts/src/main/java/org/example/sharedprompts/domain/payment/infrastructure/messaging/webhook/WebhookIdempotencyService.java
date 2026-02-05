package org.example.sharedprompts.domain.payment.infrastructure.messaging.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

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

    public void markAsProcessed(String webhookId) {
        String key = LOCK_KEY_PREFIX + webhookId;
        redisTemplate.opsForValue().set(key, LOCK_VALUE_PROCESSED, PROCESSED_TTL);
        log.debug("Webhook 처리 완료 마킹: webhookId={}", webhookId);
    }

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

    public boolean isProcessed(String webhookId) {
        String key = LOCK_KEY_PREFIX + webhookId;
        String value = redisTemplate.opsForValue().get(key);
        return LOCK_VALUE_PROCESSED.equals(value);
    }
}

