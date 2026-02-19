package org.example.sharedprompts.module.domain.production.service.ai.prompt;

import org.springframework.stereotype.Component;

/**
 * 이미지 생성용 프롬프트 빌더
 * PromptTemplateService에서 이미 병합된 프롬프트를 받아서 사용
 */
@Component
public class ImagePromptBuilder extends AbstractPromptBuilder {
    
    private static final String SYSTEM_PROMPT = 
            "Generate high-quality images based on the provided description. " +
            "The images should be detailed, visually appealing, and match the requested style.";
    
    @Override
    protected String getContentTypeName() {
        return "image";
    }
    
    @Override
    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }
}

