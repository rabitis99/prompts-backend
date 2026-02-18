package org.example.sharedprompts.module.domain.production.service.ai.config;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.service.ai.config.properties.LeonardoProperties;
import org.example.sharedprompts.module.domain.production.service.ai.retry.ExponentialBackoffRetryPolicy;
import org.example.sharedprompts.module.domain.production.service.ai.retry.RetryPolicy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Leonardo 클라이언트 설정
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.provider.leonardo.enabled", havingValue = "true")
public class LeonardoClientConfig {
    
    private final LeonardoProperties leonardoProperties;
    
    @Bean(name = "leonardoRetryPolicy")
    public RetryPolicy leonardoRetryPolicy() {
        return new ExponentialBackoffRetryPolicy(
                leonardoProperties.getMaxRetries(),
                leonardoProperties.getInitialRetryDelayMs()
        );
    }
}

