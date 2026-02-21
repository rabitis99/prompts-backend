package org.example.sharedprompts.module.domain.production.service.job.gate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Job 처리 시 외부 호출(AI/S3) 중복 방지를 위한 Redis Gate Lock.
 * 동일 job에 대해 한 번에 하나의 처리만 AI 호출 단계까지 진행하도록 합니다.
 *
 * <p>DEPLOYMENT_ISSUES 4.1 / DEPLOYMENT_RISK_REVIEW 5-2: 락 키 job:gate:{tenantId}:{jobId}, TTL로 자동 해제.
 */
@Service
@Slf4j
public class JobGateLockService {

    private static final String KEY_PREFIX = "job:gate:";

    private final StringRedisTemplate redisTemplate;
    private final boolean enabled;
    private final int ttlSeconds;

    public JobGateLockService(
            StringRedisTemplate redisTemplate,
            @Value("${production.job.gate-lock.enabled:true}") boolean enabled,
            @Value("${production.job.gate-lock.ttl-seconds:120}") int ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.enabled = enabled;
        this.ttlSeconds = ttlSeconds;
    }

    /**
     * Gate Lock 획득 시도. 성공 시에만 외부 호출(AI/S3)을 수행해야 합니다.
     *
     * @param tenantId 테넌트 ID (멀티테넌트 스코프)
     * @param jobId    Job ID
     * @return 락 획득 성공 또는 비활성화 시 true, 획득 실패 시 false
     */
    public boolean tryLock(String tenantId, String jobId) {
        if (!enabled) {
            return true;
        }
        String key = lockKey(tenantId, jobId);
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(ttlSeconds));
        if (Boolean.TRUE.equals(acquired)) {
            log.debug("Job gate lock acquired - jobId: {}, tenantId: {}", jobId, tenantId);
            return true;
        }
        log.info("Job gate lock not acquired (concurrent or Redis) - jobId: {}, tenantId: {}", jobId, tenantId);
        return false;
    }

    /**
     * Gate Lock 해제. 정상 완료/실패 후 반드시 호출합니다.
     */
    public void unlock(String tenantId, String jobId) {
        if (!enabled) {
            return;
        }
        String key = lockKey(tenantId, jobId);
        try {
            redisTemplate.delete(key);
            log.debug("Job gate lock released - jobId: {}, tenantId: {}", jobId, tenantId);
        } catch (Exception e) {
            log.warn("Job gate lock release failed (TTL will expire) - jobId: {}, tenantId: {}", jobId, tenantId, e);
        }
    }

    private static String lockKey(String tenantId, String jobId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be null or blank for gate lock key");
        }
        if (jobId == null || jobId.isBlank()) {
            throw new IllegalArgumentException("jobId must not be null or blank for gate lock key");
        }
        return KEY_PREFIX + tenantId + ":" + jobId;
    }
}
