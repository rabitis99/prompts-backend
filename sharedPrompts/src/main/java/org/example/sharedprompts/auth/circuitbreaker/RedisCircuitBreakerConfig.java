package org.example.sharedprompts.auth.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Redis Circuit Breaker 설정
 * 
 * Resilience4j를 사용하여 Redis 호출에 대한 Circuit Breaker를 구성합니다.
 * Redis 장애 시 빠르게 fallback 처리하고, 장애 반복 시 자동으로 호출을 차단합니다.
 * 
 * <p>설정 우선순위:
 * <ol>
 *   <li>application.yml의 설정이 최우선 (운영 환경 튜닝 권장)</li>
 *   <li>이 Bean의 설정은 application.yml에 설정이 없을 때만 사용</li>
 * </ol>
 * 
 * <p>tokenRedis 전용 설정 (application.yml 기준):
 * <ul>
 *   <li>실패율 임계값: 50% (20개 호출 중 10개 이상 실패 시 Circuit Breaker Open)</li>
 *   <li>느린 호출 비율 임계값: 80% (2초 이상 소요 시 느린 호출로 간주)</li>
 *   <li>Open 상태 유지 시간: 30초</li>
 *   <li>Half-Open 상태에서 허용되는 호출 수: 5개</li>
 *   <li>슬라이딩 윈도우 크기: 20개</li>
 *   <li>최소 호출 수: 20개 (통계 계산 전 최소 호출 수)</li>
 *   <li>실패로 기록할 예외: DataAccessException, ConnectException, SocketTimeoutException, JedisConnectionException</li>
 * </ul>
 * 
 * <p>Fail-Open 정책:
 * <ul>
 *   <li>읽기 작업(토큰 검증): Redis 장애 시 토큰을 유효한 것으로 간주하여 서비스 연속성 보장</li>
 *   <li>쓰기 작업(토큰 저장): Redis 장애 시 예외 발생하여 데이터 일관성 보장</li>
 * </ul>
 * 
 * <p>다른 Redis 호출에 Circuit Breaker 적용:
 * <ol>
 *   <li>application.yml에 새 인스턴스 추가 (예: cacheRedis, sessionRedis)</li>
 *   <li>@CircuitBreaker(name = "cacheRedis") 어노테이션 사용</li>
 *   <li>각 인스턴스는 독립적으로 관리되므로 서로 다른 설정 가능</li>
 * </ol>
 * 
 * <p>운영 환경 튜닝 권장 사항:
 * <ul>
 *   <li>실패율 임계값: 서비스 특성에 따라 조정 (기본 50%)</li>
 *   <li>Open 상태 유지 시간: Redis 복구 시간을 고려하여 조정 (기본 30초)</li>
 *   <li>슬라이딩 윈도우 크기: 트래픽에 따라 조정 (기본 20개)</li>
 *   <li>느린 호출 임계값: Redis 응답 시간을 고려하여 조정 (기본 2초)</li>
 * </ul>
 * 
 * 수정: CircuitBreakerRegistry 중복 생성 방지, 싱글톤 레지스트리 사용
 */
@Configuration
public class RedisCircuitBreakerConfig {

    // 수정: 싱글톤 레지스트리 관리 (동시성 안전성 보장)
    private static volatile CircuitBreakerRegistry defaultRegistry;
    private static final ReentrantLock registryLock = new ReentrantLock();
    
    // 수정: Circuit Breaker 인스턴스 캐시 (중복 생성 방지)
    private static final ConcurrentHashMap<String, CircuitBreaker> circuitBreakerCache = new ConcurrentHashMap<>();

    /**
     * Redis 전용 Circuit Breaker Bean (토큰 서비스용)
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
     * <p>다른 Redis 호출에 Circuit Breaker 적용:
     * <ul>
     *   <li>application.yml에 새 인스턴스 추가 (예: cacheRedis, sessionRedis)</li>
     *   <li>@CircuitBreaker(name = "cacheRedis") 어노테이션 사용</li>
     *   <li>각 인스턴스는 독립적으로 관리되므로 서로 다른 설정 가능</li>
     * </ul>
     * 
     * 수정: 싱글톤 레지스트리 사용, 중복 생성 방지
     * 
     * @return Redis 전용 Circuit Breaker (토큰 서비스용)
     */
    @Bean(name = "tokenRedis")
    public CircuitBreaker redisCircuitBreaker() {
        // 수정: 캐시에서 먼저 확인 (중복 생성 방지)
        return circuitBreakerCache.computeIfAbsent("tokenRedis", name -> {
            CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                    .failureRateThreshold(50) // 실패율 50% 시 차단
                    .waitDurationInOpenState(Duration.ofSeconds(30)) // Open 상태 30초 유지
                    .permittedNumberOfCallsInHalfOpenState(5) // Half-Open 상태에서 5개 호출 허용
                    .slidingWindowSize(20) // 최근 20개 호출 기준
                    .minimumNumberOfCalls(20) // 최소 20개 호출 후 통계 계산 (application.yml과 일치)
                    .recordException(throwable -> throwable instanceof DataAccessException) // DataAccessException을 실패로 기록
                    .build();

            // 수정: 싱글톤 레지스트리 사용 (중복 생성 방지)
            CircuitBreakerRegistry registry = getOrCreateDefaultRegistry(config);
            return registry.circuitBreaker(name);
        });
    }
    
    /**
     * 싱글톤 레지스트리 가져오기 또는 생성
     * 
     * 수정: Double-Checked Locking 패턴으로 동시성 안전성 보장
     * 
     * @param config Circuit Breaker 설정
     * @return Circuit Breaker 레지스트리
     */
    private static CircuitBreakerRegistry getOrCreateDefaultRegistry(CircuitBreakerConfig config) {
        if (defaultRegistry == null) {
            registryLock.lock();
            try {
                if (defaultRegistry == null) {
                    defaultRegistry = CircuitBreakerRegistry.of(config);
                }
            } finally {
                registryLock.unlock();
            }
        }
        return defaultRegistry;
    }
    
    /**
     * 공통 Redis Circuit Breaker 설정 팩토리 메서드
     * 
     * <p>다른 Redis 호출에도 Circuit Breaker를 적용할 때 사용할 수 있는 공통 설정입니다.
     * application.yml에서 각 인스턴스별로 세부 설정을 오버라이드할 수 있습니다.
     * 
     * <p>사용 예시:
     * <pre>{@code
     * // application.yml
     * resilience4j:
     *   circuitbreaker:
     *     instances:
     *       cacheRedis:
     *         failureRateThreshold: 40
     *         slidingWindowSize: 30
     * 
     * // 코드
     * @CircuitBreaker(name = "cacheRedis")
     * public void cacheOperation() { ... }
     * }</pre>
     * 
     * 수정: 싱글톤 레지스트리 사용, 중복 생성 방지
     * 
     * @param name Circuit Breaker 인스턴스 이름
     * @param config 커스텀 설정 (null이면 기본 설정 사용)
     * @return Circuit Breaker 인스턴스
     */
    public static CircuitBreaker createCircuitBreaker(String name, CircuitBreakerConfig config) {
        // 수정: 캐시에서 먼저 확인 (중복 생성 방지)
        return circuitBreakerCache.computeIfAbsent(name, n -> {
            CircuitBreakerRegistry registry;
            if (config != null) {
                // 수정: 커스텀 설정이 있으면 새 레지스트리 생성 (또는 기존 레지스트리 사용)
                registry = getOrCreateDefaultRegistry(config);
            } else {
                // 기본 설정 사용
                CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(30))
                        .permittedNumberOfCallsInHalfOpenState(5)
                        .slidingWindowSize(20)
                        .minimumNumberOfCalls(20)
                        .recordException(throwable -> throwable instanceof DataAccessException)
                        .build();
                registry = getOrCreateDefaultRegistry(defaultConfig);
            }
            return registry.circuitBreaker(n);
        });
    }
}
