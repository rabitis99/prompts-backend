package org.example.sharedprompts.global.redis;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

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
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMetrics {
    
    private final MeterRegistry meterRegistry;
    private final AtomicLong failOpenCount = new AtomicLong(0);
    
    private Counter successCounter;
    private Counter failureCounter;
    private Counter failOpenCounter;
    private Gauge healthStatusGauge;
    private Timer healthCheckTimer;
    
    /**
     * 메트릭 초기화
     * 
     * <p>애플리케이션 시작 시 메트릭을 등록합니다.
     */
    public void initialize() {
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
    }
    
    /**
     * Redis 호출 성공 기록
     */
    public void recordSuccess() {
        if (successCounter != null) {
            successCounter.increment();
        }
    }
    
    /**
     * Redis 호출 실패 기록
     */
    public void recordFailure() {
        if (failureCounter != null) {
            failureCounter.increment();
        }
    }
    
    /**
     * Fail-Open 정책 적용 기록
     */
    public void recordFailOpen() {
        if (failOpenCounter != null) {
            failOpenCounter.increment();
        }
        failOpenCount.incrementAndGet();
    }
    
    /**
     * Health Check 소요 시간 기록
     * 
     * @param duration 소요 시간 (밀리초)
     */
    public void recordHealthCheckDuration(long duration) {
        if (healthCheckTimer != null) {
            healthCheckTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * Health 상태 Gauge 등록
     * 
     * <p>Health Service와 연동하여 상태를 실시간으로 반영합니다.
     * 
     * @param healthService RedisHealthService 인스턴스
     */
    public void registerHealthStatusGauge(RedisHealthService healthService) {
        if (healthStatusGauge == null) {
            this.healthStatusGauge = Gauge.builder("redis.health.status", healthService, 
                    service -> service.getCachedHealthStatus() ? 1.0 : 0.0)
                    .description("Redis Health 상태 (0: 장애, 1: 정상)")
                    .register(meterRegistry);
        }
    }
    
    /**
     * Fail-Open 적용 총 횟수 조회
     * 
     * @return Fail-Open 적용 총 횟수
     */
    public long getFailOpenCount() {
        return failOpenCount.get();
    }
}

