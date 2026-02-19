package org.example.sharedprompts.module.domain.production.service.ai.prompt;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.ai.AIContentRequest;

/**
 * 프롬프트 빌더 추상 클래스
 * 공통 로직을 제공하며, 구현체는 getSystemPrompt()와 getContentTypeName()만 구현하면 됩니다.
 */
@Slf4j
public abstract class AbstractPromptBuilder implements PromptBuilder {
    
    @Override
    public String build(AIContentRequest request) {
        String contentTypeName = getContentTypeName();
        log.debug("Building {} prompt - promptId: {}, userId: {}, prompt length: {}", 
                contentTypeName, request.getPromptId(), maskUserId(request.getUserId()), 
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
            // PII 보호: userInput 내용을 로그에 기록하지 않고 존재 여부만 표시
            log.warn("{} prompt is null or empty - promptId: {}, userId: {}, hasUserInput: {}", 
                    contentTypeName, request.getPromptId(), maskUserId(request.getUserId()), 
                    request.getUserInput() != null && !request.getUserInput().isBlank());
        }
        
        String finalPrompt = promptBuilder.toString();
        log.debug("Final {} prompt length: {}", contentTypeName, finalPrompt.length());
        return finalPrompt;
    }
    
    /**
     * 콘텐츠 타입 이름 반환 (로깅용)
     * 예: "text", "image"
     * 
     * @return 콘텐츠 타입 이름
     */
    protected abstract String getContentTypeName();
    
    /**
     * userId를 마스킹합니다 (PII 보호).
     * 
     * @param userId 원본 사용자 ID
     * @return 마스킹된 사용자 ID (예: "12345" -> "***")
     */
    protected String maskUserId(Long userId) {
        if (userId == null) {
            return "null";
        }
        return "***";
    }
}

