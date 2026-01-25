package org.example.sharedprompts.auth.resilience;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.storage.RefreshTokenMetadata;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.redis.RedisHealthService;
import org.example.sharedprompts.global.redis.RedisMetrics;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 통합 Redis 실행기
 * 
 * <p>읽기, 쓰기, 삭제 작업을 모두 처리하는 통합 Executor입니다.
 * 작업 타입에 따라 적절한 예외 처리 및 Fail-Open 정책을 자동으로 적용합니다.
 * 
 * <p>작업 타입:
 * <ul>
 *   <li>READ: 읽기 작업 (Fail-Open 가능, 작업 타입에 따라 자동 결정)</li>
 *   <li>WRITE: 쓰기 작업 (예외 발생, 데이터 일관성 보장)</li>
 *   <li>DELETE: 삭제 작업 (예외 무시, 치명적이지 않음)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisExecutor {
    
    private final RedisHealthService redisHealthService;
    private final RedisMetrics redisMetrics;
    
    /**
     * 읽기 작업 타입
     */
    public enum ReadType {
        /** Access Token 검증: Fail-Open (true 반환) */
        ACCESS_TOKEN,
        /** Refresh Token 검증: Fail-Close (false 반환) */
        REFRESH_TOKEN,
        /** Set 조회: Fail-Open (빈 Set 반환) */
        SET,
        /** Map 조회: Fail-Open (빈 Map 반환) */
        MAP,
        /** Long 조회 (보안): Fail-Close (null 반환) */
        NULL_LONG,
        /** RefreshTokenMetadata 조회: Fail-Close (null 반환) */
        NULL_METADATA
    }
    
    /**
     * 읽기 작업 실행 (작업 타입에 따라 자동으로 Strategy 적용)
     * 
     * @param <T> 반환 타입
     * @param redisCall Redis 호출 로직
     * @param readType 읽기 작업 타입
     * @param context 로깅용 컨텍스트
     * @return Redis 호출 결과 또는 fallback 값
     */
    public <T> T executeRead(Supplier<T> redisCall, ReadType readType, String context) {
        RedisFailOpenStrategy<T> strategy = getStrategy(readType);
        return executeRead(redisCall, strategy, context);
    }
    
    /**
     * 읽기 작업 실행 (Strategy 직접 지정)
     * 
     * @param <T> 반환 타입
     * @param redisCall Redis 호출 로직
     * @param strategy Fail-Open 정책 전략
     * @param context 로깅용 컨텍스트
     * @return Redis 호출 결과 또는 fallback 값
     */
    private <T> T executeRead(
            Supplier<T> redisCall,
            RedisFailOpenStrategy<T> strategy,
            String context) {
        
        // Health Check 기반 사전 Fail-Open
        if (strategy.shouldFailOpenOnHealthCheck() 
                && !redisHealthService.getCachedHealthStatus()) {
            log.warn("Redis Health Check 장애 감지: context={}, Fail-Open 적용", context);
            redisMetrics.recordFailOpen();
            return strategy.getFallbackValue();
        }
        
        // Redis 호출
        try {
            T result = redisCall.get();
            redisMetrics.recordSuccess();
            return result;
        } catch (DataAccessException e) {
            logAtLevel(strategy.getExceptionLogLevel(), 
                    "Redis 읽기 작업 실패: context={}, Fail-Open 적용", context, e);
            redisHealthService.reportFailure();
            redisMetrics.recordFailure();
            redisMetrics.recordFailOpen();
            return strategy.getFallbackValue();
        } catch (Exception e) {
            log.error("Redis 읽기 작업 중 예상치 못한 예외 발생: context={}", context, e);
            redisHealthService.reportFailure();
            redisMetrics.recordFailure();
            redisMetrics.recordFailOpen();
            return strategy.getFallbackValue();
        }
    }
    
    /**
     * 작업 타입에 따른 Strategy 생성
     */
    @SuppressWarnings("unchecked") // ReadType에 따라 다른 타입의 Strategy 반환
    private <T> RedisFailOpenStrategy<T> getStrategy(ReadType readType) {
        return switch (readType) {
            case ACCESS_TOKEN -> (RedisFailOpenStrategy<T>) new AccessTokenStrategy();
            case REFRESH_TOKEN -> (RedisFailOpenStrategy<T>) new RefreshTokenStrategy();
            case SET -> (RedisFailOpenStrategy<T>) new EmptySetStrategy();
            case MAP -> (RedisFailOpenStrategy<T>) new EmptyMapStrategy();
            case NULL_LONG -> (RedisFailOpenStrategy<T>) new NullLongStrategy();
            case NULL_METADATA -> (RedisFailOpenStrategy<T>) new NullMetadataStrategy();
        };
    }
    
    /**
     * 쓰기 작업 실행 (예외 발생, 데이터 일관성 보장)
     * 
     * @param redisCall Redis 호출 로직
     * @param context 로깅용 컨텍스트
     * @throws ApiException Redis 장애 시 발생
     */
    public void executeWrite(Runnable redisCall, String context) {
        try {
            redisCall.run();
            redisMetrics.recordSuccess();
        } catch (DataAccessException e) {
            log.error("Redis 쓰기 작업 실패: context={}", context, e);
            redisHealthService.reportFailure();
            redisMetrics.recordFailure();
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, 
                    "Redis 장애로 인해 작업에 실패했습니다.");
        } catch (Exception e) {
            log.error("Redis 쓰기 작업 중 예상치 못한 예외 발생: context={}", context, e);
            redisHealthService.reportFailure();
            redisMetrics.recordFailure();
            throw new RuntimeException("Redis 쓰기 작업 실패: " + context, e);
        }
    }
    
    /**
     * 삭제 작업 실행 (예외 무시, 치명적이지 않음)
     * 
     * @param redisCall Redis 호출 로직
     * @param context 로깅용 컨텍스트
     */
    public void executeDelete(Runnable redisCall, String context) {
        try {
            redisCall.run();
            redisMetrics.recordSuccess();
        } catch (DataAccessException e) {
            log.warn("Redis 삭제 작업 실패: context={}, 무시하고 계속 진행", context, e);
            redisHealthService.reportFailure();
            redisMetrics.recordFailure();
            // 삭제 실패는 치명적이지 않으므로 예외를 던지지 않음
        } catch (Exception e) {
            log.warn("Redis 삭제 작업 중 예상치 못한 예외 발생: context={}, 무시하고 계속 진행", context, e);
            redisHealthService.reportFailure();
            redisMetrics.recordFailure();
            // 삭제 실패는 치명적이지 않으므로 예외를 던지지 않음
        }
    }
    
    /**
     * 로깅 레벨에 따라 로그 출력
     */
    private void logAtLevel(RedisFailOpenStrategy.LogLevel level, String message, 
                           String context, Throwable e) {
        switch (level) {
            case DEBUG -> log.debug(message, context, e);
            case INFO -> log.info(message, context, e);
            case WARN -> log.warn(message, context, e);
            case ERROR -> log.error(message, context, e);
        }
    }
    
    // ==================== 내부 Strategy 구현 ====================
    
    private static class AccessTokenStrategy implements RedisFailOpenStrategy<Boolean> {
        @Override
        public Boolean getFallbackValue() {
            return true; // Fail-Open
        }
        
        @Override
        public boolean shouldFailOpenOnHealthCheck() {
            return true;
        }
        
        @Override
        public LogLevel getExceptionLogLevel() {
            return LogLevel.WARN;
        }
    }
    
    private static class RefreshTokenStrategy implements RedisFailOpenStrategy<Boolean> {
        @Override
        public Boolean getFallbackValue() {
            return false; // Fail-Close
        }
        
        @Override
        public boolean shouldFailOpenOnHealthCheck() {
            return false;
        }
        
        @Override
        public LogLevel getExceptionLogLevel() {
            return LogLevel.ERROR;
        }
    }
    
    private static class EmptySetStrategy implements RedisFailOpenStrategy<Set<String>> {
        @Override
        public Set<String> getFallbackValue() {
            return Set.of(); // Fail-Open
        }
        
        @Override
        public boolean shouldFailOpenOnHealthCheck() {
            return true;
        }
        
        @Override
        public LogLevel getExceptionLogLevel() {
            return LogLevel.WARN;
        }
    }
    
    private static class EmptyMapStrategy implements RedisFailOpenStrategy<Map<String, String>> {
        @Override
        public Map<String, String> getFallbackValue() {
            return Map.of(); // Fail-Open
        }
        
        @Override
        public boolean shouldFailOpenOnHealthCheck() {
            return true;
        }
        
        @Override
        public LogLevel getExceptionLogLevel() {
            return LogLevel.WARN;
        }
    }
    
    private static class NullLongStrategy implements RedisFailOpenStrategy<Long> {
        @Override
        public Long getFallbackValue() {
            return null; // Fail-Close
        }
        
        @Override
        public boolean shouldFailOpenOnHealthCheck() {
            return false;
        }
        
        @Override
        public LogLevel getExceptionLogLevel() {
            return LogLevel.ERROR;
        }
    }
    
    private static class NullMetadataStrategy implements RedisFailOpenStrategy<RefreshTokenMetadata> {
        @Override
        public RefreshTokenMetadata getFallbackValue() {
            return null; // Fail-Close
        }
        
        @Override
        public boolean shouldFailOpenOnHealthCheck() {
            return false;
        }
        
        @Override
        public LogLevel getExceptionLogLevel() {
            return LogLevel.ERROR;
        }
    }
}

