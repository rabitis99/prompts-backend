package org.example.sharedprompts.module.domain.production.service.ai.image;

/**
 * Image AI 클라이언트 인터페이스
 * 실제 AI 모델과 통신하는 클라이언트 구현
 */
public interface ImageAIClient {
    
    /**
     * 이미지 생성
     */
    String generateImage(String prompt, int width, int height, String modelName);
}
