package org.example.sharedprompts.module.domain.production.service.ai.text;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.ai.AIContentRequest;
import org.example.sharedprompts.module.domain.production.model.ai.AIContentResult;
import org.example.sharedprompts.module.domain.production.service.ai.AIService;
import org.example.sharedprompts.module.domain.production.service.ai.ContentType;
import org.springframework.stereotype.Service;

/**
 * Text 콘텐츠 생성 AI 서비스
 * 블로그 글, 시, 요약 등 다양한 텍스트 콘텐츠 생성 지원
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TextAIService implements AIService {
    
    private final TextAiClient textAiClient;
    private static final String DEFAULT_MODEL = "gpt-4";
    
    @Override
    public AIContentResult generateContent(AIContentRequest request) {
        try {
            log.info("Text AI generation started - contentTypeHint: {}, model: {}", 
                    request.getContentTypeHint(), request.getModelName());
            
            // 프롬프트와 사용자 입력 조합
            String combinedPrompt = buildPrompt(request);
            
            // AI 클라이언트를 통한 텍스트 생성
            String generatedText = textAiClient.generateText(
                    combinedPrompt,
                    request.getModelName() != null ? request.getModelName() : DEFAULT_MODEL,
                    request.getContentTypeHint()
            );
            
            log.info("Text AI generation completed - length: {}", generatedText.length());
            
            return AIContentResult.success(
                    ContentType.TEXT,
                    generatedText,
                    request.getModelName() != null ? request.getModelName() : DEFAULT_MODEL
            );
            
        } catch (RuntimeException e) {
            // RuntimeException은 일시적 오류로 간주하여 예외로 전파 (retry 가능하도록)
            // TextAiClient가 던지는 예외는 대부분 RuntimeException (ApiException 등)
            log.warn("Text AI generation failed with transient error, will retry", e);
            throw e;
        } catch (Exception e) {
            // 체크 예외는 영구적 오류로 간주하여 AIContentResult.failure()로 반환
            log.error("Text AI generation failed with permanent error", e);
            return AIContentResult.failure("Text generation failed: " + e.getMessage());
        }
    }
    
    @Override
    public boolean supports(ContentType contentType) {
        return contentType == ContentType.TEXT;
    }
    
    @Override
    public String getModelName() {
        return DEFAULT_MODEL;
    }
    
    /**
     * 프롬프트와 사용자 입력을 조합하여 최종 프롬프트 생성
     */
    private String buildPrompt(AIContentRequest request) {
        StringBuilder promptBuilder = new StringBuilder();
        
        if (request.getPrompt() != null && !request.getPrompt().isBlank()) {
            promptBuilder.append(request.getPrompt());
        }
        
        if (request.getUserInput() != null && !request.getUserInput().isBlank()) {
            if (promptBuilder.length() > 0) {
                promptBuilder.append("\n\n");
            }
            promptBuilder.append("사용자 입력: ").append(request.getUserInput());
        }
        
        return promptBuilder.toString();
    }
}

