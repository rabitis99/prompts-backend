package org.example.sharedprompts.global.config.scheduler;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;

import java.util.Optional;

/**
 * Redis 장애 시 자동으로 DB 기반 LockProvider로 전환하는 Fallback LockProvider
 * 
 * 동작 방식:
 * 1. 기본적으로 Redis LockProvider 사용
 * 2. Redis 장애 발생 시 자동으로 DB LockProvider로 전환
 * 3. Redis 복구 시 다시 Redis로 전환 (다음 lock 시도 시)
 * 
 * 장애 감지:
 * - RedisConnectionFailureException
 * - DataAccessException (Redis 관련)
 * - 일반적인 RuntimeException (Redis 연결 실패 등)
 */
@Slf4j
@ConditionalOnProperty(name = "shedlock.fallback.enabled", havingValue = "true", matchIfMissing = false)
public class FallbackLockProvider implements LockProvider {

    private final LockProvider primaryLockProvider;  // Redis
    private final LockProvider fallbackLockProvider;  // DB
    private volatile boolean useFallback = false;

    public FallbackLockProvider(LockProvider primaryLockProvider, LockProvider fallbackLockProvider) {
        this.primaryLockProvider = primaryLockProvider;
        this.fallbackLockProvider = fallbackLockProvider;
    }

    @NotNull
    @Override
    public Optional<SimpleLock> lock(@NotNull LockConfiguration lockConfiguration) {
        // Always try Redis first
        try {
            Optional<SimpleLock> lock = primaryLockProvider.lock(lockConfiguration);
            if (lock.isPresent() && useFallback) {
                // Redis has recovered, switching back from DB fallback
                log.info("Redis LockProvider recovered. Switching back to Redis from DB fallback.");
                useFallback = false;
            }
            return lock; // If Redis returns Optional.empty(), don't fallback to DB
        } catch (Exception e) {
            if (isRedisFailure(e)) {
                // On Redis failure, switch to fallback
                if (!useFallback) {
                    log.warn("Redis LockProvider failed. Switching to DB fallback. Error: {}", e.getMessage());
                    useFallback = true;
                }
            } else {
                // Redis-related exceptions are handled here
                throw e;
            }
        }

        // Fallback to DB only if Redis has failed or we are in fallback mode
        try {
            Optional<SimpleLock> lock = fallbackLockProvider.lock(lockConfiguration);
            if (lock.isPresent() && useFallback) {
                log.debug("Using DB LockProvider as fallback for lock: {}", lockConfiguration.getName());
            }
            return lock;
        } catch (Exception e) {
            // Log and rethrow if DB fails
            log.error("Both Redis and DB LockProvider failed. Lock: {}", lockConfiguration.getName(), e);
            throw new RuntimeException("All LockProviders failed", e);
        }
    }


    /**
     * Redis 장애 여부 판단
     */
    private boolean isRedisFailure(Exception e) {
        return e instanceof DataAccessException
                || (e.getCause() != null && (
                e.getCause() instanceof RedisConnectionFailureException
                        || e.getCause() instanceof DataAccessException
                        || e.getCause().getClass().getName().contains("redis")
        ))
                || e.getMessage() != null && (
                e.getMessage().toLowerCase().contains("redis")
                        || e.getMessage().toLowerCase().contains("connection")
                        || e.getMessage().toLowerCase().contains("timeout")
        );
    }

    /**
     * DB 테이블이 없는 경우 에러 판단
     */
    private boolean isTableNotExistsError(Exception e) {
        if (e == null) {
            return false;
        }
        String message = e.getMessage();
        if (message == null && e.getCause() != null) {
            message = e.getCause().getMessage();
        }
        if (message == null) {
            return false;
        }
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("table") && (
            lowerMessage.contains("doesn't exist")
            || lowerMessage.contains("does not exist")
            || lowerMessage.contains("unknown table")
            || lowerMessage.contains("table '") && lowerMessage.contains("shedlock")
            || lowerMessage.contains("table") && lowerMessage.contains("shedlock") && lowerMessage.contains("not found")
        );
    }

    /**
     * 현재 사용 중인 LockProvider 상태 확인 (모니터링용)
     */
    public boolean isUsingFallback() {
        return useFallback;
    }

    /**
     * 수동으로 Fallback 모드 리셋 (테스트/복구용)
     */
    public void resetFallback() {
        log.info("Manually resetting fallback mode. Will try Redis on next lock attempt.");
        useFallback = false;
    }
}

