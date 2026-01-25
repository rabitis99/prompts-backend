package org.example.sharedprompts.scheduler.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.redis.RedisHealthService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Redis Health Check 스케줄러
 * 
 * <p>주기적으로 Redis Health Check를 수행하여 장애를 조기에 감지합니다.
 * Health Check 결과는 RedisHealthService에 캐시되어,
 * Redis 호출 전에 사전 Fail-Open 정책을 적용하는 데 사용됩니다.
 * 
 * <p>스케줄 설정:
 * <ul>
 *   <li>실행 주기: 5초마다 (fixedDelay = 5000)</li>
 *   <li>초기 지연: 10초 (initialDelay = 10000)</li>
 * </ul>
 * 
 * <p>동작 방식:
 * <ol>
 *   <li>5초마다 RedisHealthService.isRedisHealthy() 호출</li>
 *   <li>Health Check 결과가 RedisHealthService에 캐시됨</li>
 *   <li>Redis 호출 시 캐시된 Health Check 결과를 사용하여 사전 Fail-Open 적용</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisHealthCheckScheduler {
    
    private final RedisHealthService redisHealthService;
    
    /**
     * 주기적 Redis Health Check 수행
     * 
     * <p>5초마다 실행되며, Redis 연결 상태를 확인합니다.
     * Health Check 결과는 RedisHealthService에 캐시되어
     * Redis 호출 전에 사전 Fail-Open 정책을 적용하는 데 사용됩니다.
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000) // 5초마다, 시작 후 10초 지연
    public void performHealthCheck() {
        try {
            boolean isHealthy = redisHealthService.isRedisHealthy();
            if (!isHealthy) {
                log.warn("Redis Health Check: 장애 상태 감지");
            } else {
                log.debug("Redis Health Check: 정상 상태");
            }
        } catch (Exception e) {
            log.error("Redis Health Check 수행 중 예외 발생", e);
        }
    }
}

