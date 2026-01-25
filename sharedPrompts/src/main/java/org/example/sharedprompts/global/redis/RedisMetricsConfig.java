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
 * 
 * 수정: 초기화 순서 의존 제거, null 체크, 안전한 초기화
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
     * 
     * 수정: 초기화 순서 의존 제거, null 체크, 안전한 초기화
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeMetrics() {
        try {
            // 수정: null 체크 추가 (초기화 전 호출 방지)
            if (redisMetrics == null) {
                org.slf4j.LoggerFactory.getLogger(RedisMetricsConfig.class)
                        .warn("RedisMetrics가 null입니다. 초기화를 건너뜁니다.");
                return;
            }
            
            // 수정: 초기화 순서 안전성 보장 (초기화 실패해도 계속 진행)
            try {
                redisMetrics.initialize();
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(RedisMetricsConfig.class)
                        .error("Redis 메트릭 초기화 실패 (다음 단계 계속 진행)", e);
            }
            
            // 수정: Health Service null 체크
            if (redisHealthService != null) {
                try {
                    redisMetrics.registerHealthStatusGauge(redisHealthService);
                } catch (Exception e) {
                    org.slf4j.LoggerFactory.getLogger(RedisMetricsConfig.class)
                            .error("Health Status Gauge 등록 실패 (다음 단계 계속 진행)", e);
                }
                
                // 수정: 순환 참조 방지를 위해 setter로 설정 (null 체크)
                try {
                    redisHealthService.setRedisMetrics(redisMetrics);
                } catch (Exception e) {
                    org.slf4j.LoggerFactory.getLogger(RedisMetricsConfig.class)
                            .error("RedisMetrics 설정 실패 (서비스는 계속 동작)", e);
                }
            } else {
                org.slf4j.LoggerFactory.getLogger(RedisMetricsConfig.class)
                        .warn("RedisHealthService가 null입니다. 일부 기능이 동작하지 않을 수 있습니다.");
            }
        } catch (Exception e) {
            // 수정: 메트릭 초기화 실패해도 애플리케이션은 계속 동작
            // 로그만 남기고 예외를 던지지 않음
            org.slf4j.LoggerFactory.getLogger(RedisMetricsConfig.class)
                    .error("Redis 메트릭 초기화 중 예상치 못한 오류 발생 (애플리케이션은 계속 동작)", e);
        }
    }
}
