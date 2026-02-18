package org.example.sharedprompts.module.domain.production.service.ai.prompt;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.ai.AIContentRequest;
import org.springframework.stereotype.Component;

/**
 * 이미지 생성용 프롬프트 빌더
 * PromptTemplateService에서 이미 병합된 프롬프트를 받아서 사용
 */
@Component
@Slf4j
public class ImagePromptBuilder implements PromptBuilder {
    
    private static final String SYSTEM_PROMPT = 
            "Generate high-quality images based on the provided description. " +
            "The images should be detailed, visually appealing, and match the requested style.";
    
    @Override
    public String build(AIContentRequest request) {
        log.debug("Building image prompt - promptId: {}, userId: {}, prompt length: {}", 
                request.getPromptId(), request.getUserId(), 
                request.getPrompt() != null ? request.getPrompt().length() : 0);
        
        StringBuilder promptBuilder = new StringBuilder();
        
        // 시스템 프롬프트는 상수로 관리
        String systemPrompt = getSystemPrompt();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            promptBuilder.append(systemPrompt);
        }
        
        // PromptTemplateService에서 이미 병합된 프롬프트 사용 (userInput 포함)
        // PromptMerger.merge()에서 이미 프롬프트와 userInput을 병합했으므로 중복 추가하지 않음
        String mergedPrompt = request.getPrompt();
        if (mergedPrompt != null && !mergedPrompt.isBlank()) {
            if (!promptBuilder.isEmpty()) {
                promptBuilder.append("\n\n");
            }
            promptBuilder.append(mergedPrompt);
        } else {
            log.warn("Image prompt is null or empty - promptId: {}, userId: {}, userInput: {}", 
                    request.getPromptId(), request.getUserId(), request.getUserInput());
        }
        
        String finalPrompt = promptBuilder.toString();
        log.debug("Final image prompt length: {}", finalPrompt.length());
        return finalPrompt;
    }
    
    @Override
    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }
}

