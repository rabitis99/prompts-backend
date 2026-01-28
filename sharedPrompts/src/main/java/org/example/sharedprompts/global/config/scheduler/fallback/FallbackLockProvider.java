package org.example.sharedprompts.global.config.scheduler.fallback;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.Optional;

/**
 * Redis 장애 시 자동으로 DB 기반 LockProvider로 전환하는 Fallback LockProvider
 * 
 * 동작 방식:
 * 1. 기본적으로 Redis LockProvider 사용
 * 2. Redis 장애 발생 시 자동으로 DB LockProvider로 전환
 * 3. Fallback 모드에서는 Redis를 건너뛰고 DB로 바로 이동 (성능 최적화)
 * 4. Redis 복구 감지를 위해 주기적으로 Redis를 시도
 * 5. Redis 복구 시 자동으로 Redis로 전환
 */
@Slf4j
@ConditionalOnProperty(name = "shedlock.fallback.enabled", havingValue = "true", matchIfMissing = false)
public class FallbackLockProvider implements LockProvider {

    private final LockProvider primaryLockProvider;  // Redis
    private final LockProvider fallbackLockProvider;  // DB
    private volatile boolean useFallback = false;
    private final RecoveryCheckStrategy recoveryCheckStrategy;

    /**
     * 기본 생성자: 10번 중 1번 Redis 복구 체크
     */
    public FallbackLockProvider(LockProvider primaryLockProvider, LockProvider fallbackLockProvider) {
        this(primaryLockProvider, fallbackLockProvider, new PeriodicRecoveryCheckStrategy(10));
    }
    
    /**
     * 복구 체크 전략을 지정할 수 있는 생성자
     */
    public FallbackLockProvider(
            LockProvider primaryLockProvider, 
            LockProvider fallbackLockProvider,
            RecoveryCheckStrategy recoveryCheckStrategy) {
        this.primaryLockProvider = primaryLockProvider;
        this.fallbackLockProvider = fallbackLockProvider;
        this.recoveryCheckStrategy = recoveryCheckStrategy;
    }

    @NotNull
    @Override
    public Optional<SimpleLock> lock(@NotNull LockConfiguration lockConfiguration) {
        // Fallback 모드가 아닐 때는 항상 Redis를 먼저 시도
        if (!useFallback) {
            return tryRedisFirst(lockConfiguration);
        }
        
        // Fallback 모드일 때는 Redis를 건너뛰고 DB로 바로 이동
        // 단, Redis 복구 감지를 위해 주기적으로 Redis를 시도
        if (recoveryCheckStrategy.shouldCheck()) {
            log.debug("Checking Redis recovery for lock: {}", lockConfiguration.getName());
            Optional<SimpleLock> redisLock = tryRedisWithRecoveryCheck(lockConfiguration);
            if (redisLock != null && redisLock.isPresent()) {
                // Redis가 복구되어 락을 획득한 경우
                recoveryCheckStrategy.onCheckCompleted();
                return redisLock;
            }
            // Redis가 아직 복구되지 않은 경우, DB로 fallback
            recoveryCheckStrategy.onCheckCompleted();
        }
        
        // Fallback 모드: DB로 바로 이동 (Redis 타임아웃 없이)
        return tryDatabase(lockConfiguration);
    }
    
    /**
     * Redis를 먼저 시도 (정상 모드)
     */
    private Optional<SimpleLock> tryRedisFirst(LockConfiguration lockConfiguration) {
        try {
            Optional<SimpleLock> lock = primaryLockProvider.lock(lockConfiguration);
            if (lock.isPresent()) {
                return lock;
            }
            // Redis가 Optional.empty()를 반환한 경우 (락을 획득하지 못함)
            // 이는 정상적인 동작이므로 fallback하지 않음
            return lock;
        } catch (Exception e) {
            if (RedisFailureDetector.isRedisFailure(e)) {
                // Redis 장애 발생, fallback 모드로 전환
                log.warn("Redis LockProvider failed. Switching to DB fallback. Error: {}", e.getMessage());
                useFallback = true;
                recoveryCheckStrategy.reset();
                return tryDatabase(lockConfiguration);
            } else {
                // Redis 관련이 아닌 예외는 그대로 전파
                throw e;
            }
        }
    }
    
    /**
     * Redis 복구 감지를 위한 Redis 시도 (Fallback 모드에서 주기적으로 호출)
     * @return Redis 락이 획득된 경우 Optional<SimpleLock>, 그 외 null
     */
    private Optional<SimpleLock> tryRedisWithRecoveryCheck(LockConfiguration lockConfiguration) {
        try {
            Optional<SimpleLock> lock = primaryLockProvider.lock(lockConfiguration);
            if (lock.isPresent()) {
                // Redis가 복구됨!
                log.info("Redis LockProvider recovered. Switching back to Redis from DB fallback.");
                useFallback = false;
                recoveryCheckStrategy.reset();
                return lock;
            }
            // Redis가 Optional.empty()를 반환한 경우 (락을 획득하지 못함)
            // 이는 정상적인 동작이므로 fallback 모드를 유지
            return null;
        } catch (Exception e) {
            if (RedisFailureDetector.isRedisFailure(e)) {
                // Redis가 아직 복구되지 않음
                log.debug("Redis still unavailable during recovery check: {}", e.getMessage());
                return null;
            } else {
                // Redis 관련이 아닌 예외는 그대로 전파
                throw e;
            }
        }
    }
    
    /**
     * Database LockProvider로 락 획득 시도
     */
    private Optional<SimpleLock> tryDatabase(LockConfiguration lockConfiguration) {
        try {
            Optional<SimpleLock> lock = fallbackLockProvider.lock(lockConfiguration);
            if (lock.isPresent() && useFallback) {
                log.debug("Using DB LockProvider as fallback for lock: {}", lockConfiguration.getName());
            }
            return lock;
        } catch (Exception e) {
            // DB도 실패한 경우
            log.error("Both Redis and DB LockProvider failed. Lock: {}", lockConfiguration.getName(), e);
            throw new RuntimeException("All LockProviders failed", e);
        }
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
        recoveryCheckStrategy.reset();
    }
}

