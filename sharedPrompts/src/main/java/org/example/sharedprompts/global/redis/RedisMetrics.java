package org.example.sharedprompts.global.redis;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Redis 메트릭 수집기
 * 
 * <p>Redis 호출 성공/실패, Fail-Open 적용 횟수, Health Check 상태 등을 메트릭으로 수집합니다.
 * Prometheus를 통해 모니터링할 수 있습니다.
 * 
 * <p>수집 메트릭:
 * <ul>
 *   <li>redis.call.success - Redis 호출 성공 횟수</li>
 *   <li>redis.call.failure - Redis 호출 실패 횟수</li>
 *   <li>redis.call.failopen - Fail-Open 정책 적용 횟수</li>
 *   <li>redis.health.status - Health 상태 (0: 장애, 1: 정상)</li>
 *   <li>redis.health.check.duration - Health Check 소요 시간</li>
 * </ul>
 * 
 * 수정: 초기화 순서 안전성, null 체크, 동시성 안전성 강화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMetrics {
    
    private final MeterRegistry meterRegistry;
    private final AtomicLong failOpenCount = new AtomicLong(0);
    
    // 수정: volatile로 가시성 보장
    private volatile Counter successCounter;
    private volatile Counter failureCounter;
    private volatile Counter failOpenCounter;
    private volatile Gauge healthStatusGauge;
    private volatile Timer healthCheckTimer;
    
    // 수정: 동시성 안전성을 위한 lock 및 초기화 플래그
    private final ReentrantLock initLock = new ReentrantLock();
    private volatile boolean initialized = false;
    
    /**
     * 메트릭 초기화
     * 
     * <p>애플리케이션 시작 시 메트릭을 등록합니다.
     * 
     * <p>중복 등록 방지:
     * <ul>
     *   <li>synchronized 블록으로 동시성 제어</li>
     *   <li>initialized 플래그로 이중 초기화 방지</li>
     *   <li>Gauge/Timer 등록 시 중복 체크 수행</li>
     * </ul>
     * 
     * 수정: 초기화 실패 시에도 서비스 계속 동작, null 체크 강화
     */
    public void initialize() {
        // 수정: Double-Checked Locking 패턴으로 동시성 안전성 보장
        if (initialized) {
            log.debug("Redis 메트릭이 이미 초기화되었습니다. 중복 초기화 방지");
            return;
        }
        
        initLock.lock();
        try {
            if (initialized) {
                log.debug("Redis 메트릭이 이미 초기화되었습니다. 중복 초기화 방지");
                return;
            }
            
            try {
                // Redis 호출 성공/실패 카운터
                this.successCounter = Counter.builder("redis.call.success")
                        .description("Redis 호출 성공 횟수")
                        .register(meterRegistry);
                
                this.failureCounter = Counter.builder("redis.call.failure")
                        .description("Redis 호출 실패 횟수")
                        .register(meterRegistry);
                
                this.failOpenCounter = Counter.builder("redis.call.failopen")
                        .description("Fail-Open 정책 적용 횟수")
                        .register(meterRegistry);
                
                // Health Check 타이머
                this.healthCheckTimer = Timer.builder("redis.health.check.duration")
                        .description("Redis Health Check 소요 시간")
                        .register(meterRegistry);
                
                initialized = true;
                log.debug("Redis 메트릭 초기화 완료");
            } catch (Exception e) {
                // 수정: 초기화 실패해도 서비스는 계속 동작
                log.error("Redis 메트릭 초기화 실패 (서비스는 계속 동작)", e);
                // 예외를 던지지 않음
            }
        } finally {
            initLock.unlock();
        }
    }
    
    /**
     * Redis 호출 성공 기록
     * 
     * 수정: null 체크 추가 (초기화 전 호출 방지)
     */
    public void recordSuccess() {
        Counter counter = successCounter;
        if (counter != null) {
            counter.increment();
        }
    }
    
    /**
     * Redis 호출 실패 기록
     * 
     * 수정: null 체크 추가 (초기화 전 호출 방지)
     */
    public void recordFailure() {
        Counter counter = failureCounter;
        if (counter != null) {
            counter.increment();
        }
    }
    
    /**
     * Fail-Open 정책 적용 기록
     * 
     * 수정: null 체크 추가 (초기화 전 호출 방지)
     */
    public void recordFailOpen() {
        Counter counter = failOpenCounter;
        if (counter != null) {
            counter.increment();
        }
        failOpenCount.incrementAndGet();
    }
    
    /**
     * Health Check 소요 시간 기록
     * 
     * 수정: null 체크 추가 (초기화 전 호출 방지)
     * 
     * @param duration 소요 시간 (밀리초)
     */
    public void recordHealthCheckDuration(long duration) {
        Timer timer = healthCheckTimer;
        if (timer != null) {
            timer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * Health 상태 Gauge 등록
     * 
     * <p>Health Service와 연동하여 상태를 실시간으로 반영합니다.
     * 
     * <p>중복 등록 방지:
     * <ul>
     *   <li>synchronized 블록으로 동시성 제어</li>
     *   <li>healthStatusGauge null 체크로 이중 등록 방지</li>
     *   <li>Gauge는 MeterRegistry에 등록되면 자동으로 관리됨</li>
     * </ul>
     * 
     * 수정: 초기화 순서 안전성, null 체크, 동시성 안전성 강화
     * 
     * @param healthService RedisHealthService 인스턴스
     */
    public synchronized void registerHealthStatusGauge(RedisHealthService healthService) {
        // 수정: null 체크 추가
        if (healthService == null) {
            log.warn("Health Service가 null입니다. Gauge 등록을 건너뜁니다.");
            return;
        }
        
        if (healthStatusGauge != null) {
            log.debug("Health Status Gauge가 이미 등록되었습니다. 중복 등록 방지");
            return;
        }
        
        try {
            this.healthStatusGauge = Gauge.builder("redis.health.status", healthService, 
                    service -> {
                        // 수정: null-safe 호출
                        if (service == null) {
                            return 0.0;
                        }
                        return service.getCachedHealthStatus() ? 1.0 : 0.0;
                    })
                    .description("Redis Health 상태 (0: 장애, 1: 정상)")
                    .register(meterRegistry);
            log.debug("Health Status Gauge 등록 완료");
        } catch (Exception e) {
            // 수정: 등록 실패해도 서비스는 계속 동작
            log.error("Health Status Gauge 등록 실패 (서비스는 계속 동작)", e);
            // 예외를 던지지 않음
        }
    }
    
    /**
     * Fail-Open 적용 총 횟수 조회
     * 
     * 수정: 동시성 안전성 보장 (AtomicLong 사용)
     * 
     * @return Fail-Open 적용 총 횟수
     */
    public long getFailOpenCount() {
        return failOpenCount.get();
    }
}
