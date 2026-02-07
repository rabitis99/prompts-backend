package org.example.sharedprompts.global.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisHealthService {

    private final StringRedisTemplate redisTemplate;
    private final RedisHealthCheckProperties healthCheckProperties;
    private volatile RedisMetrics redisMetrics;
    
    private final AtomicBoolean isHealthy = new AtomicBoolean(true);
    private final AtomicLong lastCheckTime = new AtomicLong(0);
    private final AtomicBoolean healthGaugeRegistered = new AtomicBoolean(false);
    private final AtomicLong consecutiveFailures = new AtomicLong(0);
    private final AtomicLong downSinceTime = new AtomicLong(0);
    private final AtomicLong adaptiveBackoffMs = new AtomicLong(0);
    private static final long MIN_TTL_MS = 1000;
    private static final long MAX_BACKOFF_MS = 30_000;
    private final ReentrantLock healthCheckLock = new ReentrantLock();
    private volatile boolean isChecking = false;
    
    public synchronized void setRedisMetrics(RedisMetrics redisMetrics) {
        this.redisMetrics = redisMetrics;
        registerHealthGaugeIfPossible();
    }

    private void registerHealthGaugeIfPossible() {
        RedisMetrics metrics = this.redisMetrics;
        if (metrics != null && healthGaugeRegistered.compareAndSet(false, true)) {
            metrics.registerHealthStatusGauge(this);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try {
            registerHealthGaugeIfPossible();
        } catch (Exception e) {
            log.error("Redis Health Service 초기화 실패 (서비스는 계속 동작)", e);
        }
    }
    
    public boolean isRedisHealthy() {
        if (!healthCheckProperties.isEnabled()) {
            return true;
        }
        
        long currentTime = System.currentTimeMillis();
        long lastCheck = lastCheckTime.get();
        
        if (currentTime - lastCheck < MIN_TTL_MS && lastCheck > 0) {
            return isHealthy.get();
        }
        
        long effectiveInterval = calculateEffectiveInterval();
        
        if (currentTime - lastCheck < effectiveInterval && lastCheck > 0) {
            return isHealthy.get();
        }
        
        return performHealthCheck();
    }
    
    private long calculateEffectiveInterval() {
        long baseInterval = healthCheckProperties.getIntervalMs();
        long backoff = adaptiveBackoffMs.get();
        
        if (backoff > 0) {
            return Math.min(baseInterval + backoff, MAX_BACKOFF_MS);
        }
        
        return baseInterval;
    }
    
    private boolean performHealthCheck() {
        if (!healthCheckLock.tryLock()) {
            return isHealthy.get();
        }
        
        try {
            isChecking = true;
            
            long startTime = System.currentTimeMillis();
            try {
                String result = redisTemplate.execute((RedisCallback<String>) connection -> {
                    return connection.ping();
                });
                
                long duration = System.currentTimeMillis() - startTime;
                
                if ("PONG".equals(result)) {
                    consecutiveFailures.set(0);
                    isHealthy.set(true);
                    adaptiveBackoffMs.set(0);
                    downSinceTime.set(0);
                    lastCheckTime.set(System.currentTimeMillis());
                    log.debug("Redis Health Check: 정상");
                    
                    if (redisMetrics != null) {
                        redisMetrics.recordHealthCheckDuration(duration);
                    }
                    
                    return true;
                } else {
                    handleHealthCheckFailure();
                    if (redisMetrics != null) {
                        redisMetrics.recordHealthCheckDuration(duration);
                    }
                    return false;
                }
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                handleHealthCheckFailure();
                log.warn("Redis Health Check 실패", e);
                
                if (redisMetrics != null) {
                    redisMetrics.recordHealthCheckDuration(duration);
                }
                
                return false;
            }
        } finally {
            isChecking = false;
            healthCheckLock.unlock();
        }
    }
    
    private void handleHealthCheckFailure() {
        long failures = consecutiveFailures.incrementAndGet();
        lastCheckTime.set(System.currentTimeMillis());
        
        long maxFailures = Math.max(1, healthCheckProperties.getMaxConsecutiveFailures());
        long backoff = Math.min((failures / maxFailures) * 1000, MAX_BACKOFF_MS);
        adaptiveBackoffMs.set(backoff);

        if (isHealthy.compareAndSet(true, false)) {
            downSinceTime.set(System.currentTimeMillis());
            log.error("Redis 장애 감지: Health Check 실패 (연속 실패 횟수: {}, backoff: {}ms)", failures, backoff);
        } else if (failures >= maxFailures) {
            log.error("Redis 장애 지속: 연속 {}회 Health Check 실패 (backoff: {}ms)", failures, backoff);
        }
    }
    
    public void reportFailure() {
        handleHealthCheckFailure();
    }
    
    public boolean forceHealthCheck() {
        return performHealthCheck();
    }
    
    public boolean getCachedHealthStatus() {
        return isHealthy.get();
    }
    
    public void setHealthy(boolean healthy) {
        isHealthy.set(healthy);
        if (healthy) {
            consecutiveFailures.set(0);
            adaptiveBackoffMs.set(0);
            downSinceTime.set(0);
        } else {
            downSinceTime.set(System.currentTimeMillis());
        }
        lastCheckTime.set(System.currentTimeMillis());
    }
    
    public long getConsecutiveFailures() {
        return consecutiveFailures.get();
    }
    
    public long getDownSinceTime() {
        return downSinceTime.get();
    }
}
