package org.example.sharedprompts.module.domain.production.service.ai.text;

/**
 * Text AI 클라이언트 인터페이스
 * 실제 AI 모델과 통신하는 클라이언트 구현
 */
public interface TextAiClient {
    
    /**
     * 텍스트 생성
     */
    String generateText(String prompt, String modelName, String contentTypeHint);
}
