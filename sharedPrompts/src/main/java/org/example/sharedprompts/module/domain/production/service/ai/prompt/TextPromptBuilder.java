package org.example.sharedprompts.module.domain.production.service.ai.prompt;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.ai.AIContentRequest;
import org.springframework.stereotype.Component;

/**
 * 텍스트 생성용 프롬프트 빌더
 * PromptTemplateService에서 이미 병합된 프롬프트를 받아서 사용
 */
@Component
@Slf4j
public class TextPromptBuilder implements PromptBuilder {
    
    private static final String SYSTEM_PROMPT = 
            "You are a helpful assistant that generates high-quality text content. " +
            "Follow the user's instructions carefully and provide detailed, well-structured responses.";
    
    @Override
    public String build(AIContentRequest request) {
        StringBuilder promptBuilder = new StringBuilder();
        
        // 시스템 프롬프트는 상수로 관리
        String systemPrompt = getSystemPrompt();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            promptBuilder.append(systemPrompt);
        }
        
        // PromptTemplateService에서 이미 병합된 프롬프트 사용 (userInput 포함)
        // PromptMerger.merge()에서 이미 프롬프트와 userInput을 병합했으므로 중복 추가하지 않음
        if (request.getPrompt() != null && !request.getPrompt().isBlank()) {
            if (!promptBuilder.isEmpty()) {
                promptBuilder.append("\n\n");
            }
            promptBuilder.append(request.getPrompt());
        }
        
        return promptBuilder.toString();
    }
    
    @Override
    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }
}

