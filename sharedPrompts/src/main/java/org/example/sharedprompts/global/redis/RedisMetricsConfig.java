package org.example.sharedprompts.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Redis 메트릭 초기화 설정
 * 
 * <p>애플리케이션 시작 시 Redis 메트릭을 초기화합니다.
 */
@Component
@RequiredArgsConstructor
public class RedisMetricsConfig {
    
    private final RedisMetrics redisMetrics;
    private final RedisHealthService redisHealthService;
    
    /**
     * 애플리케이션 시작 시 메트릭 초기화
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeMetrics() {
        redisMetrics.initialize();
        redisMetrics.registerHealthStatusGauge(redisHealthService);
        // 순환 참조 방지를 위해 setter로 설정
        redisHealthService.setRedisMetrics(redisMetrics);
    }
}

