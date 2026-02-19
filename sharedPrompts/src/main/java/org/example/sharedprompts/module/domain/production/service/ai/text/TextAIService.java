package org.example.sharedprompts.module.domain.production.service.ai.text;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.ai.AIContentRequest;
import org.example.sharedprompts.module.domain.production.model.ai.AIContentResult;
import org.example.sharedprompts.module.domain.production.service.ai.AIService;
import org.example.sharedprompts.module.domain.production.service.ai.ContentType;
import org.example.sharedprompts.module.domain.production.service.ai.exception.AiClientException;
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
            // 현재: 시스템 프롬프트와 사용자 프롬프트가 병합되어 단일 문자열로 전달됨
            String combinedPrompt = promptBuilder.build(request);
            
            // 모델 이름 결정 (요청에 없으면 전략에서 가져옴, 없으면 기본값)
            String modelName = determineModelName(request);
            
            // AI 클라이언트를 통한 텍스트 생성
            // TODO: 시스템 프롬프트와 사용자 프롬프트 분리 전달 개선
            // - 목적: Groq Chat Completion API에서 system/user 역할 분리로 모델 응답 품질 향상
            // - 방법: TextPromptBuilder.getSystemPrompt()를 별도로 전달
            // - 구현: GroqChatRequest(model, systemPrompt, userPrompt) 생성자 사용
            // - 참고: GroqTextAiClient.generateText() 시그니처 변경 필요
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
            
        } catch (AiClientException e) {
            // AiClientException은 API 호출 실패로 인한 일시적 오류로 간주하여 예외로 전파 (retry 가능하도록)
            log.warn("Text AI generation failed with transient error, will retry", e);
            throw e;
        } catch (RuntimeException e) {
            // IllegalArgumentException, NullPointerException 등은 입력 검증 실패로 인한 영구적 오류
            // 재시도해도 해결되지 않으므로 즉시 실패 처리
            log.error("Text AI generation failed with permanent error (validation/configuration issue)", e);
            return AIContentResult.failure("Text generation failed: " + e.getMessage());
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
    public Optional<ContentType> getSupportedContentType() {
        return Optional.of(ContentType.TEXT);
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

