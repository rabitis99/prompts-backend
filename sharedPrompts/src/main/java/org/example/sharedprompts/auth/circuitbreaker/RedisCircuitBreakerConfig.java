package org.example.sharedprompts.auth.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis Circuit Breaker 설정
 * 
 * Resilience4j를 사용하여 Redis 호출에 대한 Circuit Breaker를 구성합니다.
 * Redis 장애 시 빠르게 fallback 처리하고, 장애 반복 시 자동으로 호출을 차단합니다.
 * 
 * <p>설정 우선순위:
 * <ol>
 *   <li>application.yml의 설정이 최우선 (운영 환경 튜닝 권장)</li>
 *   <li>Spring Boot 자동 설정이 application.yml을 바인딩하여 CircuitBreakerRegistry 생성</li>
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
 * 수정: Spring Boot 자동 설정 CircuitBreakerRegistry 주입 사용
 * - application.yml 설정이 정상적으로 적용됨
 * - 운영 환경에서 application.yml을 통한 튜닝 가능
 */
@Configuration
@RequiredArgsConstructor
public class RedisCircuitBreakerConfig {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * Redis 전용 Circuit Breaker Bean (토큰 서비스용)
     * 
     * <p>Spring Boot 자동 설정이 application.yml의 설정을 바인딩하여 생성한
     * CircuitBreakerRegistry를 사용합니다. 따라서 application.yml의 설정이 정상적으로 적용됩니다.
     * 
     * <p>설정 값 (application.yml 기준):
     * <ul>
     *   <li>failureRateThreshold: 50% - 실패율이 50%를 초과하면 Circuit Breaker Open</li>
     *   <li>waitDurationInOpenState: 30초 - Open 상태에서 30초 후 Half-Open으로 전환</li>
     *   <li>permittedNumberOfCallsInHalfOpenState: 5개 - Half-Open 상태에서 5개 호출 허용</li>
     *   <li>slidingWindowSize: 20개 - 최근 20개 호출을 기준으로 실패율 계산</li>
     *   <li>slowCallRateThreshold: 80% - 느린 호출 비율 80% 시 Circuit Breaker Open</li>
     *   <li>slowCallDurationThreshold: 2초 - 2초 이상 소요 시 느린 호출로 간주</li>
     * </ul>
     * 
     * <p>다른 Redis 호출에 Circuit Breaker 적용:
     * <ul>
     *   <li>application.yml에 새 인스턴스 추가 (예: cacheRedis, sessionRedis)</li>
     *   <li>@CircuitBreaker(name = "cacheRedis") 어노테이션 사용</li>
     *   <li>각 인스턴스는 독립적으로 관리되므로 서로 다른 설정 가능</li>
     * </ul>
     * 
     * <p>운영 환경 튜닝:
     * <ul>
     *   <li>application.yml의 resilience4j.circuitbreaker.instances.tokenRedis 설정을 수정하여
     *       운영 환경에 맞게 튜닝 가능</li>
     *   <li>재시작 없이도 일부 설정 변경 가능 (동적 설정 지원 시)</li>
     * </ul>
     * 
     * @return Redis 전용 Circuit Breaker (토큰 서비스용)
     */
    @Bean(name = "tokenRedis")
    public CircuitBreaker redisCircuitBreaker() {
        // Spring Boot 자동 설정이 application.yml을 바인딩한 레지스트리 사용
        // application.yml의 설정이 정상적으로 적용됨
        return circuitBreakerRegistry.circuitBreaker("tokenRedis");
    }
}
