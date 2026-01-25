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
    
    // Adaptive backoff를 위한 변수
    private final AtomicLong adaptiveBackoffMs = new AtomicLong(0);
    private static final long MIN_TTL_MS = 1000; // 최소 TTL: 1초
    private static final long MAX_BACKOFF_MS = 30_000; // 최대 backoff: 30초
    
    /**
     * RedisMetrics 설정 (선택적 의존성)
     * 
     * <p>순환 참조 방지를 위해 setter injection 사용
     */
    public void setRedisMetrics(RedisMetrics redisMetrics) {
        this.redisMetrics = redisMetrics;
    }
    
    /**
     * ApplicationReadyEvent에서 초기화
     * 
     * <p>PostConstruct 대신 ApplicationReadyEvent를 사용하여 초기화 순서 안정화
     * 모든 Bean이 준비된 후에 초기화되므로 순환 참조 문제를 방지합니다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        // Health Status Gauge 등록
        if (redisMetrics != null) {
            redisMetrics.registerHealthStatusGauge(this);
        }
    }
    
    /**
     * Redis 연결 상태 확인
     * 
     * <p>Adaptive backoff 및 최소 TTL 보장:
     * <ul>
     *   <li>정상 상태: 기본 interval 사용</li>
     *   <li>장애 상태: adaptive backoff 적용 (최대 30초)</li>
     *   <li>최소 TTL: 1초 (stale 데이터 최소화)</li>
     * </ul>
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
        
        // 최소 TTL 보장: 마지막 체크 후 최소 1초는 경과해야 함
        if (currentTime - lastCheck < MIN_TTL_MS && lastCheck > 0) {
            return isHealthy.get();
        }
        
        // Adaptive backoff 계산
        long effectiveInterval = calculateEffectiveInterval();
        
        // 최근에 체크했고 아직 인터벌이 지나지 않았다면 캐시된 결과 반환
        if (currentTime - lastCheck < effectiveInterval && lastCheck > 0) {
            return isHealthy.get();
        }
        
        // 실제 Health Check 수행
        return performHealthCheck();
    }
    
    /**
     * Adaptive backoff를 고려한 유효한 인터벌 계산
     * 
     * <p>장애 상태일 때는 backoff를 적용하여 Health Check 빈도를 줄입니다.
     * 정상 상태일 때는 기본 interval을 사용합니다.
     * 
     * @return 유효한 인터벌 (밀리초)
     */
    private long calculateEffectiveInterval() {
        long baseInterval = healthCheckProperties.getIntervalMs();
        long backoff = adaptiveBackoffMs.get();
        
        if (backoff > 0) {
            // 장애 상태: base interval + backoff (최대 30초)
            return Math.min(baseInterval + backoff, MAX_BACKOFF_MS);
        }
        
        // 정상 상태: base interval만 사용
        return baseInterval;
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
                adaptiveBackoffMs.set(0); // Backoff 초기화
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
     * 
     * <p>Adaptive backoff 적용:
     * - 연속 실패 횟수에 따라 backoff 시간 증가
     * - 최대 30초까지 증가
     */
    private void handleHealthCheckFailure() {
        long failures = consecutiveFailures.incrementAndGet();
        lastCheckTime.set(System.currentTimeMillis());
        
        // Adaptive backoff 계산: 실패 횟수에 따라 지수적으로 증가
        // 예: 3회 실패 → 1초, 6회 실패 → 2초, 9회 실패 → 4초, ...
        long backoff = Math.min((failures / healthCheckProperties.getMaxConsecutiveFailures()) * 1000, MAX_BACKOFF_MS);
        adaptiveBackoffMs.set(backoff);
        
        if (failures >= healthCheckProperties.getMaxConsecutiveFailures()) {
            if (isHealthy.compareAndSet(true, false)) {
                log.error("Redis 장애 감지: 연속 {}회 Health Check 실패 (backoff: {}ms)", failures, backoff);
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
            adaptiveBackoffMs.set(0); // Backoff 초기화
        }
        lastCheckTime.set(System.currentTimeMillis());
    }
}

