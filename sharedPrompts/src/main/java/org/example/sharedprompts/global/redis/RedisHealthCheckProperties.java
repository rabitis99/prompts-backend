package org.example.sharedprompts.global.redis;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Redis Health Check 설정
 * 
 * <p>application.yml에서 설정을 읽어옵니다.
 * 
 * <p>설정 예시:
 * <pre>{@code
 * redis:
 *   health-check:
 *     enabled: true
 *     interval-ms: 5000
 *     max-consecutive-failures: 3
 * }</pre>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "redis.health-check")
public class RedisHealthCheckProperties {
    
    /**
     * Health Check 활성화 여부
     * 
     * <p>기본값: true
     */
    private boolean enabled = true;
    
    /**
     * Health Check 주기 (밀리초)
     * 
     * <p>기본값: 5000 (5초)
     */
    private long intervalMs = 5_000;
    
    /**
     * 연속 실패 횟수 임계값
     * 
     * <p>이 횟수만큼 연속 실패하면 Redis를 장애 상태로 간주합니다.
     * 기본값: 3
     */
    private long maxConsecutiveFailures = 3;
}

