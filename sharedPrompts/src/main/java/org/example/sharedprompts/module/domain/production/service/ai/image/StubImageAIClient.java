package org.example.sharedprompts.module.domain.production.service.ai.image;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Image AI 클라이언트 스텁 구현
 * 실제 AI 클라이언트 구현이 없을 때 사용되는 기본 구현
 * TODO: 실제 AI 클라이언트 구현으로 교체 필요
 */
@Component
@ConditionalOnMissingBean(LeonardoImageAiClient.class)
@Slf4j
public class StubImageAIClient implements ImageAIClient {
    
    @Override
    public String generateImage(String prompt, int width, int height, String modelName) {
        log.warn("StubImageAIClient is being used. Image generation is not implemented yet. " +
                "Prompt: {}, Size: {}x{}, Model: {}", prompt, width, height, modelName);
        
        // 실제 구현이 필요할 때까지 예외를 던지거나 플레이스홀더 반환
        throw new UnsupportedOperationException(
                "ImageAIClient implementation is not available. " +
                "Please provide a concrete implementation of ImageAIClient interface."
        );
    }
}

