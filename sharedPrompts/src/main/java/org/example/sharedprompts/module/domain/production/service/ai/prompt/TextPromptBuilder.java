package org.example.sharedprompts.module.domain.production.service.ai.prompt;

import org.springframework.stereotype.Component;

/**
 * 텍스트 생성용 프롬프트 빌더
 * PromptTemplateService에서 이미 병합된 프롬프트를 받아서 사용
 */
@Component
public class TextPromptBuilder extends AbstractPromptBuilder {
    
    private static final String SYSTEM_PROMPT = 
            "You are a helpful assistant that generates high-quality text content. " +
            "Follow the user's instructions carefully and provide detailed, well-structured responses.";
    
    @Override
    protected String getContentTypeName() {
        return "text";
    }
    
    @Override
    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }
}

