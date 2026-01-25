package org.example.sharedprompts.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Redis 메트릭 초기화 설정
 * 
 * <p>애플리케이션 시작 시 Redis 메트릭을 초기화합니다.
 * 
 * <p>초기화 순서:
 * <ol>
 *   <li>ApplicationReadyEvent 발생 시 메트릭 초기화</li>
 *   <li>Health Status Gauge 등록</li>
 *   <li>순환 참조 방지를 위해 setter로 RedisMetrics 설정</li>
 * </ol>
 * 
 * <p>테스트 환경:
 * <ul>
 *   <li>@MockBean으로 RedisHealthService를 모킹할 수 있음</li>
 *   <li>setter 주입은 선택적이므로 테스트에서 생략 가능</li>
 *   <li>ApplicationReadyEvent는 테스트에서도 발생하므로 주의 필요</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class RedisMetricsConfig {
    
    private final RedisMetrics redisMetrics;
    private final RedisHealthService redisHealthService;
    
    /**
     * 애플리케이션 시작 시 메트릭 초기화
     * 
     * <p>ApplicationReadyEvent를 사용하여 모든 Bean이 준비된 후 초기화
     * 순환 참조 방지를 위해 setter injection 사용
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeMetrics() {
        try {
            redisMetrics.initialize();
            redisMetrics.registerHealthStatusGauge(redisHealthService);
            // 순환 참조 방지를 위해 setter로 설정
            // 테스트 환경에서도 동작하도록 null 체크 없이 설정
            redisHealthService.setRedisMetrics(redisMetrics);
        } catch (Exception e) {
            // 메트릭 초기화 실패해도 애플리케이션은 계속 동작
            // 로그만 남기고 예외를 던지지 않음
            org.slf4j.LoggerFactory.getLogger(RedisMetricsConfig.class)
                    .error("Redis 메트릭 초기화 실패 (애플리케이션은 계속 동작)", e);
        }
    }
}

