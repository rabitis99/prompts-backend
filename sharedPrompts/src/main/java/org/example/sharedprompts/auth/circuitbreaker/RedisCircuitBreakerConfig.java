package org.example.sharedprompts.auth.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;

import java.time.Duration;

/**
 * Redis Circuit Breaker 설정
 * 
 * Resilience4j를 사용하여 Redis 호출에 대한 Circuit Breaker를 구성합니다.
 * Redis 장애 시 빠르게 fallback 처리하고, 장애 반복 시 자동으로 호출을 차단합니다.
 * 
 * <p>주의: application.yml의 설정이 우선순위가 높습니다.
 * 실제 사용되는 설정은 application.yml의 resilience4j.circuitbreaker.instances.tokenRedis를 참조하세요.
 * 
 * <p>설정 내용 (application.yml 기준):
 * <ul>
 *   <li>실패율 임계값: 50% (20개 호출 중 10개 이상 실패 시 Circuit Breaker Open)</li>
 *   <li>Open 상태 유지 시간: 30초</li>
 *   <li>Half-Open 상태에서 허용되는 호출 수: 5개</li>
 *   <li>슬라이딩 윈도우 크기: 20개</li>
 *   <li>DataAccessException을 실패로 기록</li>
 * </ul>
 * 
 * <p>Fail-Open 정책:
 * <ul>
 *   <li>읽기 작업(토큰 검증): Redis 장애 시 토큰을 유효한 것으로 간주하여 서비스 연속성 보장</li>
 *   <li>쓰기 작업(토큰 저장): Redis 장애 시 예외 발생하여 데이터 일관성 보장</li>
 * </ul>
 */
@Configuration
public class RedisCircuitBreakerConfig {

    /**
     * Redis 전용 Circuit Breaker Bean
     * 
     * <p>주의: application.yml의 설정이 우선순위가 높습니다.
     * 이 Bean은 application.yml에 설정이 없을 때만 사용됩니다.
     * 
     * <p>설정 값 (application.yml 기준):
     * <ul>
     *   <li>failureRateThreshold: 50% - 실패율이 50%를 초과하면 Circuit Breaker Open</li>
     *   <li>waitDurationInOpenState: 30초 - Open 상태에서 30초 후 Half-Open으로 전환</li>
     *   <li>permittedNumberOfCallsInHalfOpenState: 5개 - Half-Open 상태에서 5개 호출 허용</li>
     *   <li>slidingWindowSize: 20개 - 최근 20개 호출을 기준으로 실패율 계산</li>
     *   <li>recordException: DataAccessException을 실패로 기록</li>
     * </ul>
     * 
     * @return Redis 전용 Circuit Breaker
     */
    @Bean(name = "tokenRedis")
    public CircuitBreaker redisCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // 실패율 50% 시 차단
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Open 상태 30초 유지
                .permittedNumberOfCallsInHalfOpenState(5) // Half-Open 상태에서 5개 호출 허용
                .slidingWindowSize(20) // 최근 20개 호출 기준
                .minimumNumberOfCalls(20) // 최소 20개 호출 후 통계 계산 (application.yml과 일치)
                .recordException(throwable -> throwable instanceof DataAccessException) // DataAccessException을 실패로 기록
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        return registry.circuitBreaker("tokenRedis");
    }
}

