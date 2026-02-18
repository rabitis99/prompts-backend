package org.example.sharedprompts.module.domain.production.service.ai.prompt;

import org.example.sharedprompts.module.domain.production.model.ai.AIContentRequest;

/**
 * 프롬프트 빌더 인터페이스
 * 시스템 프롬프트와 사용자 프롬프트를 조합하여 최종 프롬프트 생성
 */
public interface PromptBuilder {
    
    /**
     * 요청으로부터 최종 프롬프트 생성
     */
    String build(AIContentRequest request);
    
    /**
     * 시스템 프롬프트 반환 (상수)
     */
    String getSystemPrompt();
}

