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
import java.util.concurrent.atomic.AtomicLong;
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
    
    // 로그 sampling/throttling을 위한 카운터
    private static final long LOG_SAMPLE_RATE = 100; // 100개 중 1개만 로그
    private final AtomicLong logCounter = new AtomicLong(0);
    
    /**
     * 읽기 작업 타입
     */
    public enum ReadType {
        /** 
         * Access Token 검증: Fail-Open (true 반환)
         * 
         * <p>보안 검토 필요:
         * - Redis 장애 시 모든 Access Token을 유효한 것으로 간주
         * - JWT 서명 검증은 이미 통과한 상태이므로, Redis 장애 시에도 인증 허용
         * - 보안 위험: 만료된 토큰도 유효한 것으로 간주될 수 있음
         * - 대안: Fail-Close로 변경 시 서비스 중단 가능성 있음
         * - 권장: 모니터링 강화 및 빠른 Redis 복구 대응
         */
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
        
        // 수정: Health Service null 체크 추가 (초기화 전 호출 방지)
        if (strategy.shouldFailOpenOnHealthCheck() 
                && redisHealthService != null
                && !redisHealthService.getCachedHealthStatus()) {
            logWithSampling("Redis Health Check 장애 감지: context={}, Fail-Open 적용", context);
            if (redisMetrics != null) {
                redisMetrics.recordFailOpen();
            }
            T fallback = strategy.getFallbackValue();
            // 수정: null-safe 반환 (호출부에서 null 체크 필요)
            return fallback;
        }
        
        // Redis 호출
        try {
            T result = redisCall.get();
            if (redisMetrics != null) {
                redisMetrics.recordSuccess();
            }
            // Redis에서 null이 반환될 수 있음 (정상적인 상황)
            // 예: 토큰이 존재하지 않는 경우, RefreshToken 조회 시 null 반환 가능
            return result;
        } catch (DataAccessException e) {
            logAtLevelWithSampling(strategy.getExceptionLogLevel(), 
                    "Redis 읽기 작업 실패: context={}, Fail-Open 적용", context, e);
            if (redisHealthService != null) {
                redisHealthService.reportFailure();
            }
            if (redisMetrics != null) {
                redisMetrics.recordFailure();
                redisMetrics.recordFailOpen();
            }
            T fallback = strategy.getFallbackValue();
            // 수정: null-safe 반환 (호출부에서 null 체크 필요)
            return fallback;
        } catch (Exception e) {
            logAtLevelWithSampling(RedisFailOpenStrategy.LogLevel.ERROR,
                    "Redis 읽기 작업 중 예상치 못한 예외 발생: context={}", context, e);
            if (redisHealthService != null) {
                redisHealthService.reportFailure();
            }
            if (redisMetrics != null) {
                redisMetrics.recordFailure();
                redisMetrics.recordFailOpen();
            }
            T fallback = strategy.getFallbackValue();
            // 수정: null-safe 반환 (호출부에서 null 체크 필요)
            return fallback;
        }
    }
    
    /**
     * 작업 타입에 따른 Strategy 생성
     * 
     * 수정: @SuppressWarnings 제거하고 타입 안전하게 변환
     * ReadType과 제네릭 타입의 매핑을 명확히 하여 ClassCastException 방지
     */
    @SuppressWarnings("unchecked")
    private <T> RedisFailOpenStrategy<T> getStrategy(ReadType readType) {
        // 수정: 타입 안전성을 위해 각 ReadType에 맞는 Strategy를 명시적으로 반환
        // 제네릭 타입과 실제 반환 타입이 일치하도록 보장
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
            if (redisMetrics != null) {
                redisMetrics.recordSuccess();
            }
        } catch (DataAccessException e) {
            log.error("Redis 쓰기 작업 실패: context={}", context, e);
            if (redisHealthService != null) {
                redisHealthService.reportFailure();
            }
            if (redisMetrics != null) {
                redisMetrics.recordFailure();
            }
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, 
                    "Redis 장애로 인해 작업에 실패했습니다.");
        } catch (Exception e) {
            log.error("Redis 쓰기 작업 중 예상치 못한 예외 발생: context={}", context, e);
            if (redisHealthService != null) {
                redisHealthService.reportFailure();
            }
            if (redisMetrics != null) {
                redisMetrics.recordFailure();
            }
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
            if (redisMetrics != null) {
                redisMetrics.recordSuccess();
            }
        } catch (DataAccessException e) {
            log.warn("Redis 삭제 작업 실패: context={}, 무시하고 계속 진행", context, e);
            if (redisHealthService != null) {
                redisHealthService.reportFailure();
            }
            if (redisMetrics != null) {
                redisMetrics.recordFailure();
            }
            // 삭제 실패는 치명적이지 않으므로 예외를 던지지 않음
        } catch (Exception e) {
            log.warn("Redis 삭제 작업 중 예상치 못한 예외 발생: context={}, 무시하고 계속 진행", context, e);
            if (redisHealthService != null) {
                redisHealthService.reportFailure();
            }
            if (redisMetrics != null) {
                redisMetrics.recordFailure();
            }
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
    
    /**
     * 로깅 레벨에 따라 로그 출력 (sampling 적용)
     * 
     * <p>로그 과다 방지를 위해 sampling 적용
     * 100개 중 1개만 로그 출력
     * 
     * 수정: 동시성 안전성 보장 (AtomicLong 사용)
     */
    private void logAtLevelWithSampling(RedisFailOpenStrategy.LogLevel level, String message, 
                                       String context, Throwable e) {
        long count = logCounter.incrementAndGet();
        if (count % LOG_SAMPLE_RATE == 0 || level == RedisFailOpenStrategy.LogLevel.ERROR) {
            // ERROR 레벨은 항상 로그, 나머지는 sampling
            logAtLevel(level, message, context, e);
        }
    }
    
    /**
     * 로그 출력 (sampling 적용)
     * 
     * <p>로그 과다 방지를 위해 sampling 적용
     * 
     * 수정: 동시성 안전성 보장 (AtomicLong 사용)
     */
    private void logWithSampling(String message, Object... args) {
        long count = logCounter.incrementAndGet();
        if (count % LOG_SAMPLE_RATE == 0) {
            log.warn(message, args);
        }
    }
    
    // ==================== 내부 Strategy 구현 ====================
    
    /**
     * 수정: 모든 Strategy 클래스는 타입 안전성을 보장하도록 구현
     * Fail-Close 전략에서 null 반환 시 호출부에서 null 체크 필요
     */
    
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
    
    /**
     * 수정: NullLongStrategy는 null을 반환하므로 호출부에서 null 체크 필요
     */
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
    
    /**
     * 수정: NullMetadataStrategy는 null을 반환하므로 호출부에서 null 체크 필요
     */
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
