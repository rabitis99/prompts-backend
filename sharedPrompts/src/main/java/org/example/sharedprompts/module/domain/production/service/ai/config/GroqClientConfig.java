package org.example.sharedprompts.module.domain.production.service.ai.config;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.service.ai.config.properties.GroqProperties;
import org.example.sharedprompts.module.domain.production.service.ai.retry.ExponentialBackoffRetryPolicy;
import org.example.sharedprompts.module.domain.production.service.ai.retry.RetryPolicy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Groq 클라이언트 설정
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.provider.groq.enabled", havingValue = "true")
public class GroqClientConfig {
    
    private final GroqProperties groqProperties;
    
    @Bean(name = "groqRetryPolicy")
    public RetryPolicy groqRetryPolicy() {
        return new ExponentialBackoffRetryPolicy(
                groqProperties.getMaxRetries(),
                groqProperties.getInitialRetryDelayMs()
        );
    }
}

