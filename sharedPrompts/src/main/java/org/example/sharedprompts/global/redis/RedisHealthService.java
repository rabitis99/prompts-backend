package org.example.sharedprompts.global.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis Health Check 서비스
 * 
 * Redis 연결 상태를 주기적으로 확인하고, 장애 상태를 추적합니다.
 * Circuit Breaker와 연동하여 Fail-Open 정책을 지원합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisHealthService {

    private final StringRedisTemplate redisTemplate;
    private final RedisHealthCheckProperties healthCheckProperties;
    private RedisMetrics redisMetrics; // 선택적 의존성
    
    private final AtomicBoolean isHealthy = new AtomicBoolean(true);
    private final AtomicLong lastCheckTime = new AtomicLong(0);
    private final AtomicLong consecutiveFailures = new AtomicLong(0);
    
    /**
     * RedisMetrics 설정 (선택적 의존성)
     * 
     * <p>순환 참조 방지를 위해 setter injection 사용
     */
    public void setRedisMetrics(RedisMetrics redisMetrics) {
        this.redisMetrics = redisMetrics;
    }
    
    @PostConstruct
    public void init() {
        // Health Status Gauge 등록
        if (redisMetrics != null) {
            redisMetrics.registerHealthStatusGauge(this);
        }
    }
    
    /**
     * Redis 연결 상태 확인
     * 
     * @return Redis가 정상 상태이면 true, 장애 상태이면 false
     */
    public boolean isRedisHealthy() {
        // Health Check가 비활성화된 경우 항상 정상으로 간주
        if (!healthCheckProperties.isEnabled()) {
            return true;
        }
        
        long currentTime = System.currentTimeMillis();
        long lastCheck = lastCheckTime.get();
        
        // 최근에 체크했고 아직 인터벌이 지나지 않았다면 캐시된 결과 반환
        if (currentTime - lastCheck < healthCheckProperties.getIntervalMs() && lastCheck > 0) {
            return isHealthy.get();
        }
        
        // 실제 Health Check 수행
        return performHealthCheck();
    }
    
    /**
     * 실제 Health Check 수행
     */
    private boolean performHealthCheck() {
        long startTime = System.currentTimeMillis();
        try {
            // PING 명령으로 연결 상태 확인
            String result = redisTemplate.execute((RedisCallback<String>) connection -> {
                return connection.ping();
            });
            
            long duration = System.currentTimeMillis() - startTime;
            
            if ("PONG".equals(result)) {
                // 정상 상태
                consecutiveFailures.set(0);
                isHealthy.set(true);
                lastCheckTime.set(System.currentTimeMillis());
                log.debug("Redis Health Check: 정상");
                
                // 메트릭 기록
                if (redisMetrics != null) {
                    redisMetrics.recordHealthCheckDuration(duration);
                }
                
                return true;
            } else {
                // 비정상 응답
                handleHealthCheckFailure();
                if (redisMetrics != null) {
                    redisMetrics.recordHealthCheckDuration(duration);
                }
                return false;
            }
        } catch (Exception e) {
            // 예외 발생 시 장애 처리
            long duration = System.currentTimeMillis() - startTime;
            handleHealthCheckFailure();
            log.warn("Redis Health Check 실패: {}", e.getMessage());
            
            // 메트릭 기록
            if (redisMetrics != null) {
                redisMetrics.recordHealthCheckDuration(duration);
            }
            
            return false;
        }
    }
    
    /**
     * Health Check 실패 처리
     */
    private void handleHealthCheckFailure() {
        long failures = consecutiveFailures.incrementAndGet();
        lastCheckTime.set(System.currentTimeMillis());
        
        if (failures >= healthCheckProperties.getMaxConsecutiveFailures()) {
            if (isHealthy.compareAndSet(true, false)) {
                log.error("Redis 장애 감지: 연속 {}회 Health Check 실패", failures);
            }
        }
    }
    
    /**
     * 외부에서 Health Check 실패를 보고할 때 사용
     * (예: DataAccessException 발생 시)
     */
    public void reportFailure() {
        handleHealthCheckFailure();
    }
    
    /**
     * 강제로 Health Check 수행 (캐시 무시)
     * 
     * @return Redis가 정상 상태이면 true
     */
    public boolean forceHealthCheck() {
        return performHealthCheck();
    }
    
    /**
     * 현재 Health 상태 반환 (캐시된 값)
     * 
     * @return 마지막 Health Check 결과
     */
    public boolean getCachedHealthStatus() {
        return isHealthy.get();
    }
    
    /**
     * Health 상태를 강제로 설정 (복구 시 사용)
     */
    public void setHealthy(boolean healthy) {
        isHealthy.set(healthy);
        if (healthy) {
            consecutiveFailures.set(0);
        }
        lastCheckTime.set(System.currentTimeMillis());
    }
}

