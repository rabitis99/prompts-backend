package org.example.sharedprompts.domain.payment.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Webhook 멱등성 관리 서비스
 * 
 * <p>Redis를 사용하여 Webhook ID를 저장하고 중복 처리 방지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookIdempotencyService {
    
    private static final String WEBHOOK_IDEMPOTENCY_KEY_PREFIX = "webhook:idempotency:";
    private static final Duration WEBHOOK_IDEMPOTENCY_TTL = Duration.ofDays(7); // 7일간 보관
    
    private final RedisTemplate<String, String> redisTemplate;
    
    /**
     * Webhook이 이미 처리되었는지 확인
     * 
     * @param webhookId Webhook ID
     * @return 이미 처리된 경우 true
     */
    public boolean isAlreadyProcessed(String webhookId) {
        String key = WEBHOOK_IDEMPOTENCY_KEY_PREFIX + webhookId;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }
    
    /**
     * Webhook을 처리 완료로 표시
     * 
     * @param webhookId Webhook ID
     */
    public void markAsProcessed(String webhookId) {
        String key = WEBHOOK_IDEMPOTENCY_KEY_PREFIX + webhookId;
        redisTemplate.opsForValue().set(key, "processed", WEBHOOK_IDEMPOTENCY_TTL);
        log.debug("Webhook 멱등성 키 저장: webhookId={}", webhookId);
    }
}

