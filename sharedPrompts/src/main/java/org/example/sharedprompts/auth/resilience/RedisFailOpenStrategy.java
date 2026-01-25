package org.example.sharedprompts.auth.resilience;

/**
 * Redis Fail-Open 정책 전략 인터페이스
 * 
 * <p>다양한 토큰 타입에 따라 다른 Fail-Open 정책을 적용하기 위한 전략 패턴입니다.
 * 
 * <p>정책 종류:
 * <ul>
 *   <li>Fail-Open: Redis 장애 시 fallback 값 반환 (서비스 연속성 보장)</li>
 *   <li>Fail-Close: Redis 장애 시 false/null 반환 (보안 보장)</li>
 * </ul>
 * 
 * @param <T> 반환 타입 (Boolean, Long, Set 등)
 */
public interface RedisFailOpenStrategy<T> {
    
    /**
     * Redis 장애 시 반환할 fallback 값
     * 
     * @return fallback 값
     */
    T getFallbackValue();
    
    /**
     * Health Check 장애 감지 시 사전 Fail-Open 적용 여부
     * 
     * <p>true인 경우: Health Check가 장애를 감지하면 Redis 호출 전에 즉시 fallback 값 반환
     * <p>false인 경우: Health Check 장애와 관계없이 Redis 호출 시도
     * 
     * @return 사전 Fail-Open 적용 여부
     */
    boolean shouldFailOpenOnHealthCheck();
    
    /**
     * 예외 발생 시 로깅 레벨
     * 
     * @return 로깅 레벨 (WARN: 읽기 작업, ERROR: 쓰기 작업)
     */
    LogLevel getExceptionLogLevel();
    
    /**
     * 로깅 레벨 enum
     */
    enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
}

