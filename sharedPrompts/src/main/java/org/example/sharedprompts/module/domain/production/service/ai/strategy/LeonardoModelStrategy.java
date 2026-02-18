package org.example.sharedprompts.module.domain.production.service.ai.strategy;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.service.ai.config.properties.LeonardoProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Leonardo 모델 전략
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.provider.leonardo.enabled", havingValue = "true")
public class LeonardoModelStrategy implements AIModelStrategy {
    
    private final LeonardoProperties leonardoProperties;
    
    @Override
    public String getModelName() {
        return leonardoProperties.getDefaultModelId();
    }
    
    @Override
    public boolean isDefault() {
        return true;
    }
}

