package org.example.sharedprompts.module.domain.production.service.ai.text;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.ai.AIContentRequest;
import org.example.sharedprompts.module.domain.production.model.ai.AIContentResult;
import org.example.sharedprompts.module.domain.production.service.ai.AIService;
import org.example.sharedprompts.module.domain.production.service.ai.ContentType;
import org.example.sharedprompts.module.domain.production.service.ai.prompt.TextPromptBuilder;
import org.example.sharedprompts.module.domain.production.service.ai.strategy.AIModelStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Text 콘텐츠 생성 AI 서비스
 * 블로그 글, 시, 요약 등 다양한 텍스트 콘텐츠 생성 지원
 */
@Service
@Slf4j
public class TextAIService implements AIService {
    
    private static final String DEFAULT_MODEL = "gpt-4";
    
    private final TextAiClient textAiClient;
    private final TextPromptBuilder promptBuilder;
    private final AIModelStrategy modelStrategy;
    
    /**
     * 생성자
     * 
     * @param textAiClient 텍스트 AI 클라이언트 (필수)
     * @param promptBuilder 프롬프트 빌더 (필수)
     * @param modelStrategy 모델 전략 (선택적, null 가능)
     */
    public TextAIService(
            TextAiClient textAiClient,
            TextPromptBuilder promptBuilder,
            @Qualifier("groqModelStrategy") @Nullable AIModelStrategy modelStrategy) {
        this.textAiClient = textAiClient;
        this.promptBuilder = promptBuilder;
        this.modelStrategy = modelStrategy;
    }
    
    @Override
    public AIContentResult generateContent(AIContentRequest request) {
        try {
            log.info("Text AI generation started - contentTypeHint: {}, model: {}", 
                    request.getContentTypeHint(), request.getModelName());
            
            // 프롬프트 빌더를 사용하여 최종 프롬프트 생성
            String combinedPrompt = promptBuilder.build(request);
            
            // 모델 이름 결정 (요청에 없으면 전략에서 가져옴, 없으면 기본값)
            String modelName = determineModelName(request);
            
            // AI 클라이언트를 통한 텍스트 생성
            // 향후 개선: 시스템 프롬프트와 사용자 프롬프트를 분리하여 전달
            // GroqChatRequest의 새로운 생성자(model, systemPrompt, userPrompt) 사용 가능
            String generatedText = textAiClient.generateText(
                    combinedPrompt,
                    modelName,
                    request.getContentTypeHint()
            );
            
            log.info("Text AI generation completed - length: {}", generatedText.length());
            
            return AIContentResult.success(
                    ContentType.TEXT,
                    generatedText,
                    modelName
            );
            
        } catch (RuntimeException e) {
            // RuntimeException (AiClientException 포함)은 일시적 오류로 간주하여 예외로 전파 (retry 가능하도록)
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
    public ContentType getSupportedContentType() {
        return ContentType.TEXT;
    }
    
    @Override
    public String getModelName() {
        return determineModelName(null);
    }
    
    /**
     * 모델 이름 결정
     * 요청에 모델 이름이 있으면 사용, 없으면 전략에서 가져오고, 그것도 없으면 기본값 사용
     */
    private String determineModelName(AIContentRequest request) {
        if (request != null && request.getModelName() != null && !request.getModelName().isBlank()) {
            return request.getModelName();
        }
        return Optional.ofNullable(modelStrategy)
                .map(AIModelStrategy::getModelName)
                .orElse(DEFAULT_MODEL);
    }
}

