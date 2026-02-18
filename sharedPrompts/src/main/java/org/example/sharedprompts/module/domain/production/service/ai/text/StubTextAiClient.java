package org.example.sharedprompts.module.domain.production.service.ai.text;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.service.ai.text.GroqTextAiClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Text AI 클라이언트 스텁 구현
 * 실제 AI 클라이언트 구현이 없을 때 사용되는 기본 구현
 * TODO: 실제 AI 클라이언트 구현으로 교체 필요
 */
@Component
@ConditionalOnMissingBean(GroqTextAiClient.class)
@Slf4j
public class StubTextAiClient implements TextAiClient {
    
    @Override
    public String generateText(String prompt, String modelName, String contentTypeHint) {
        log.warn("StubTextAiClient is being used. Text generation is not implemented yet. " +
                "Prompt: {}, Model: {}, ContentTypeHint: {}", prompt, modelName, contentTypeHint);
        
        // 실제 구현이 필요할 때까지 예외를 던지거나 플레이스홀더 반환
        throw new UnsupportedOperationException(
                "TextAiClient implementation is not available. " +
                "Please provide a concrete implementation of TextAiClient interface."
        );
    }
}

