package org.example.sharedprompts.global.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.resilience.RedisExecutor;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisFailSafeHandler {
    
    private final RedisExecutor redisExecutor;
    private final RedisHealthService redisHealthService;
    private final RedisMetrics redisMetrics;
    
    /**
     * 읽기 작업 실행 (작업 타입에 따라 자동으로 Strategy 적용)
     * 
     * <p>ReadType에 따라 적절한 Fail-Open/Fail-Close 정책이 자동으로 적용됩니다.
     * 
     * @param <T> 반환 타입
     * @param redisCall Redis 호출 로직
     * @param readType 읽기 작업 타입
     * @param context 로깅용 컨텍스트
     * @return Redis 호출 결과 또는 fallback 값
     */
    public <T> T executeRead(Supplier<T> redisCall, RedisExecutor.ReadType readType, String context) {
        return redisExecutor.executeRead(redisCall, readType, context);
    }
    
    /**
     * 쓰기 작업 실행 (예외 발생, 데이터 일관성 보장)
     * 
     * <p>Redis 장애 시 예외를 발생시켜 데이터 일관성을 보장합니다.
     * 
     * @param redisCall Redis 호출 로직
     * @param context 로깅용 컨텍스트
     * @throws RuntimeException Redis 장애 시 발생
     */
    public void executeWrite(Runnable redisCall, String context) {
        redisExecutor.executeWrite(redisCall, context);
    }
    
    /**
     * 삭제 작업 실행 (예외 무시, 치명적이지 않음)
     * 
     * <p>삭제 실패는 치명적이지 않으므로 Redis 장애 시에도 예외를 던지지 않습니다.
     * 
     * @param redisCall Redis 호출 로직
     * @param context 로깅용 컨텍스트
     */
    public void executeDelete(Runnable redisCall, String context) {
        redisExecutor.executeDelete(redisCall, context);
    }
    
    /**
     * 현재 Redis Health 상태 조회
     * 
     * <p>Health Check 서비스를 통해 Redis의 현재 상태를 확인합니다.
     * 
     * @return Redis가 정상 상태이면 true
     */
    public boolean isRedisHealthy() {
        if (redisHealthService == null) {
            log.debug("RedisHealthService가 null입니다. Health 상태를 확인할 수 없습니다.");
            return true; // Health Service가 없으면 정상으로 간주
        }
        return redisHealthService.isRedisHealthy();
    }
    
    /**
     * Fail-Open 정책 적용 횟수 조회
     * 
     * <p>모니터링 및 알림에 사용할 수 있습니다.
     * 
     * @return Fail-Open 적용 총 횟수
     */
    public long getFailOpenCount() {
        if (redisMetrics == null) {
            return 0L;
        }
        return redisMetrics.getFailOpenCount();
    }
}

