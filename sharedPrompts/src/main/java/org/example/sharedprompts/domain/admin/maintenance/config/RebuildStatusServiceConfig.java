package org.example.sharedprompts.domain.admin.maintenance.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.sharedprompts.domain.admin.maintenance.rebuild.global.GlobalRebuildStatusService;
import org.example.sharedprompts.domain.admin.maintenance.rebuild.LocalRebuildStatusService;
import org.example.sharedprompts.domain.admin.maintenance.service.RebuildStatusService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * RebuildStatusService 설정
 * useGlobalStatus 설정에 따라 글로벌 또는 로컬 상태 관리 서비스를 선택합니다.
 */
@Configuration
public class RebuildStatusServiceConfig {
    
    /**
     * 글로벌 상태 관리 서비스 (Redis 기반)
     * useGlobalStatus가 true일 때만 활성화
     */
    @Bean
    @Primary
    @ConditionalOnProperty(
            name = "admin.maintenance.rebuild.use-global-status",
            havingValue = "true",
            matchIfMissing = false
    )
    public RebuildStatusService globalRebuildStatusService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        return new GlobalRebuildStatusService(redisTemplate, objectMapper);
    }
    
    /**
     * 로컬 상태 관리 서비스 (메모리 기반)
     * useGlobalStatus가 false이거나 설정되지 않았을 때 사용
     */
    @Bean
    @Primary
    @ConditionalOnProperty(
            name = "admin.maintenance.rebuild.use-global-status",
            havingValue = "false",
            matchIfMissing = true
    )
    public RebuildStatusService localRebuildStatusService() {
        return new LocalRebuildStatusService();
    }
}

