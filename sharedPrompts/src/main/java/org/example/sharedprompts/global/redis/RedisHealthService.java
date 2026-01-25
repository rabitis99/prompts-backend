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

/**
 * Redis Health Check 서비스
 * 
 * Redis 연결 상태를 주기적으로 확인하고, 장애 상태를 추적합니다.
 * Circuit Breaker와 연동하여 Fail-Open 정책을 지원합니다.
 * 
 * 수정: 동시성 안전성 강화, Health Check 중복 호출 방지, race condition 제거
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisHealthService {

    private final StringRedisTemplate redisTemplate;
    private final RedisHealthCheckProperties healthCheckProperties;
    private volatile RedisMetrics redisMetrics; // 수정: volatile로 가시성 보장
    
    private final AtomicBoolean isHealthy = new AtomicBoolean(true);
    private final AtomicLong lastCheckTime = new AtomicLong(0);
    private final AtomicLong consecutiveFailures = new AtomicLong(0);
    
    // 수정: Adaptive backoff를 위한 변수 (volatile로 가시성 보장)
    private final AtomicLong adaptiveBackoffMs = new AtomicLong(0);
    private static final long MIN_TTL_MS = 1000; // 최소 TTL: 1초
    private static final long MAX_BACKOFF_MS = 30_000; // 최대 backoff: 30초
    
    // 수정: Health Check 중복 호출 방지를 위한 lock
    private final ReentrantLock healthCheckLock = new ReentrantLock();
    private volatile boolean isChecking = false;
    
    /**
     * RedisMetrics 설정 (선택적 의존성)
     * 
     * <p>순환 참조 방지를 위해 setter injection 사용
     * 
     * 수정: 동시성 안전성을 위해 synchronized 사용
     */
    public synchronized void setRedisMetrics(RedisMetrics redisMetrics) {
        this.redisMetrics = redisMetrics;
    }
    
    /**
     * ApplicationReadyEvent에서 초기화
     * 
     * <p>PostConstruct 대신 ApplicationReadyEvent를 사용하여 초기화 순서 안정화
     * 모든 Bean이 준비된 후에 초기화되므로 순환 참조 문제를 방지합니다.
     * 
     * 수정: 초기화 실패 시에도 서비스 계속 동작
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try {
            // Health Status Gauge 등록
            if (redisMetrics != null) {
                redisMetrics.registerHealthStatusGauge(this);
            }
        } catch (Exception e) {
            // 수정: 초기화 실패해도 서비스는 계속 동작
            log.error("Redis Health Service 초기화 실패 (서비스는 계속 동작)", e);
        }
    }
    
    /**
     * Redis 연결 상태 확인
     * 
     * <p>Adaptive backoff 및 최소 TTL 보장으로 stale 데이터 최소화:
     * <ul>
     *   <li>정상 상태: 기본 interval 사용 (기본 5초)</li>
     *   <li>장애 상태: adaptive backoff 적용 (최대 30초)</li>
     *   <li>최소 TTL: 1초 (stale 데이터 최소화)</li>
     *   <li>캐시 정확성: 최소 TTL과 adaptive backoff로 최신 상태 보장</li>
     * </ul>
     * 
     * <p>캐시 정확성 보장:
     * <ul>
     *   <li>최소 TTL(1초) 이내에는 캐시된 결과 반환 (과도한 Health Check 방지)</li>
     *   <li>최소 TTL 경과 후에는 adaptive backoff를 고려한 interval 적용</li>
     *   <li>장애 상태일 때는 backoff를 증가시켜 Health Check 빈도 감소</li>
     * </ul>
     * 
     * 수정: 동시성 안전성 강화, 중복 호출 방지
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
        
        // 수정: 중복 호출 방지 (lock 사용)
        if (isChecking) {
            // 다른 스레드가 체크 중이면 캐시된 결과 반환
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
     * 수정: 동시성 안전성 보장 (AtomicLong 사용)
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
     * 
     * 수정: 동시성 안전성 강화, 중복 호출 방지, race condition 제거
     */
    private boolean performHealthCheck() {
        // 수정: lock을 사용하여 중복 호출 방지
        if (!healthCheckLock.tryLock()) {
            // 다른 스레드가 체크 중이면 캐시된 결과 반환
            return isHealthy.get();
        }
        
        try {
            // 수정: 중복 체크 방지 플래그 설정
            isChecking = true;
            
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
                    
                    // 메트릭 기록 (null 체크)
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
                
                // 메트릭 기록 (null 체크)
                if (redisMetrics != null) {
                    redisMetrics.recordHealthCheckDuration(duration);
                }
                
                return false;
            }
        } finally {
            // 수정: lock 해제 및 플래그 초기화
            isChecking = false;
            healthCheckLock.unlock();
        }
    }
    
    /**
     * Health Check 실패 처리
     * 
     * <p>Adaptive backoff 적용:
     * - 연속 실패 횟수에 따라 backoff 시간 증가
     * - 최대 30초까지 증가
     * 
     * 수정: 동시성 안전성 보장 (AtomicLong 사용)
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
     * 
     * 수정: 동시성 안전성 보장
     */
    public void reportFailure() {
        handleHealthCheckFailure();
    }
    
    /**
     * 강제로 Health Check 수행 (캐시 무시)
     * 
     * 수정: 동시성 안전성 보장
     * 
     * @return Redis가 정상 상태이면 true
     */
    public boolean forceHealthCheck() {
        return performHealthCheck();
    }
    
    /**
     * 현재 Health 상태 반환 (캐시된 값)
     * 
     * 수정: 동시성 안전성 보장 (AtomicBoolean 사용)
     * 
     * @return 마지막 Health Check 결과
     */
    public boolean getCachedHealthStatus() {
        return isHealthy.get();
    }
    
    /**
     * Health 상태를 강제로 설정 (복구 시 사용)
     * 
     * <p>테스트 환경에서 사용 가능:
     * <ul>
     *   <li>@MockBean으로 RedisHealthService를 모킹할 수 있음</li>
     *   <li>setter 주입은 선택적이므로 테스트에서 생략 가능</li>
     *   <li>이 메서드를 통해 테스트에서 Health 상태를 강제로 설정 가능</li>
     * </ul>
     * 
     * 수정: 동시성 안전성 보장
     * 
     * @param healthy Health 상태 (true: 정상, false: 장애)
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
