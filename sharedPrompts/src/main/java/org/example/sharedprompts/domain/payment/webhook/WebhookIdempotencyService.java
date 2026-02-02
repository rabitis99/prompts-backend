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
 *
 * <p><strong>알려진 제한사항 - 동시성 제어 일관성:</strong>
 * - WebhookIdempotencyService: RedisTemplate 직접 사용 (setIfAbsent)
 * - PointServiceImpl: ShedLock (LockProvider) 사용
 * - CashbackLockService: ShedLock (LockProvider) 사용
 * - 서로 다른 방식으로 동시성 제어하여 일관성 부족
 *
 * <p><strong>권장 개선사항:</strong>
 * - 통일된 DistributedLockService 또는 IdempotencyService 인터페이스 도입
 * - Redis 직접 사용 대신 추상화 계층을 통해 접근
 * - 모든 동시성 제어를 동일한 메커니즘으로 통일
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookIdempotencyService {
    
    private static final String WEBHOOK_IDEMPOTENCY_KEY_PREFIX = "webhook:idempotency:";
    private static final Duration WEBHOOK_IDEMPOTENCY_TTL = Duration.ofDays(7); // 7일간 보관
    
    private final RedisTemplate<String, String> redisTemplate;
    
    /**
     * Webhook 처리 시도 (원자적 체크 + 마킹)
     *
     * <p>TOCTOU 레이스 컨디션을 방지하기 위해 setIfAbsent를 사용하여
     * 원자적으로 체크와 마킹을 수행합니다.
     *
     * @param webhookId Webhook ID
     * @return 처리를 진행해야 하면 true, 이미 처리 중이거나 완료된 경우 false
     */
    public boolean tryProcess(String webhookId) {
        String key = WEBHOOK_IDEMPOTENCY_KEY_PREFIX + webhookId;
        // setIfAbsent는 키가 없을 때만 설정하고 true 반환, 이미 존재하면 false 반환 (원자적 연산)
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, "processing", WEBHOOK_IDEMPOTENCY_TTL);
        boolean acquired = Boolean.TRUE.equals(result);

        if (acquired) {
            log.debug("Webhook 처리 락 획득: webhookId={}", webhookId);
        } else {
            log.debug("Webhook 이미 처리 중 또는 완료: webhookId={}", webhookId);
        }

        return acquired;
    }

    /**
     * Webhook이 이미 처리되었는지 확인
     *
     * @param webhookId Webhook ID
     * @return 이미 처리된 경우 true
     * @deprecated tryProcess() 메서드 사용 권장 (원자적 체크+마킹)
     */
    @Deprecated
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
        log.debug("Webhook 처리 완료 표시: webhookId={}", webhookId);
    }

    /**
     * Webhook 처리 실패 시 락 해제
     *
     * @param webhookId Webhook ID
     */
    public void releaseProcessingLock(String webhookId) {
        String key = WEBHOOK_IDEMPOTENCY_KEY_PREFIX + webhookId;
        String value = redisTemplate.opsForValue().get(key);
        // "processing" 상태일 때만 삭제 (완료된 것은 삭제하지 않음)
        if ("processing".equals(value)) {
            redisTemplate.delete(key);
            log.debug("Webhook 처리 락 해제: webhookId={}", webhookId);
        }
    }
}

