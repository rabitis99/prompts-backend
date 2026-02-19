package org.example.sharedprompts.module.domain.production.service.ai.strategy;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.service.ai.ContentType;
import org.example.sharedprompts.module.domain.production.service.ai.config.properties.GroqProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Groq 모델 전략
 * 텍스트 생성용 AI 모델 전략
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.provider.groq.enabled", havingValue = "true")
public class GroqModelStrategy implements AIModelStrategy {
    
    private final GroqProperties groqProperties;
    
    @Override
    public String getModelName() {
        return groqProperties.getDefaultModelId();
    }
    
    @Override
    public boolean isDefault() {
        return true;
    }
    
    @Override
    public ContentType getSupportedContentType() {
        return ContentType.TEXT;
    }
}

