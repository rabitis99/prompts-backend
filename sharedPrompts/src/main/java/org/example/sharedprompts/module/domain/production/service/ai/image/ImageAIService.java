package org.example.sharedprompts.module.domain.production.service.ai.image;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.ai.AIContentRequest;
import org.example.sharedprompts.module.domain.production.model.ai.AIContentResult;
import org.example.sharedprompts.module.domain.production.service.ai.AIService;
import org.example.sharedprompts.module.domain.production.service.ai.ContentType;
import org.springframework.stereotype.Service;

/**
 * Image 콘텐츠 생성 AI 서비스
 * 다양한 이미지 생성 모델 지원
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageAIService implements AIService {
    
    private final ImageAIClient imageAIClient;
    private static final String DEFAULT_MODEL = "dall-e-3";
    
    @Override
    public AIContentResult generateContent(AIContentRequest request) {
        try {
            log.info("Image AI generation started - prompt: {}, size: {}x{}, model: {}", 
                    request.getPrompt(), request.getWidth(), request.getHeight(), request.getModelName());
            
            // 프롬프트와 사용자 입력 조합
            String combinedPrompt = buildPrompt(request);
            
            // AI 클라이언트를 통한 이미지 생성
            String imagePath = imageAIClient.generateImage(
                    combinedPrompt,
                    request.getWidth() != null ? request.getWidth() : 1024,
                    request.getHeight() != null ? request.getHeight() : 1024,
                    request.getModelName() != null ? request.getModelName() : DEFAULT_MODEL
            );
            
            log.info("Image AI generation completed - path: {}", imagePath);
            
            return AIContentResult.success(
                    ContentType.IMAGE,
                    imagePath, // 이미지 파일 경로 또는 base64
                    request.getModelName() != null ? request.getModelName() : DEFAULT_MODEL
            );
            
        } catch (RuntimeException e) {
            // RuntimeException은 일시적 오류로 간주하여 예외로 전파 (retry 가능하도록)
            // ImageAiClient가 던지는 예외는 대부분 RuntimeException (ApiException 등)
            log.warn("Image AI generation failed with transient error, will retry", e);
            throw e;
        } catch (Exception e) {
            // 체크 예외는 영구적 오류로 간주하여 AIContentResult.failure()로 반환
            log.error("Image AI generation failed with permanent error", e);
            return AIContentResult.failure("Image generation failed: " + e.getMessage());
        }
    }
    
    @Override
    public boolean supports(ContentType contentType) {
        return contentType == ContentType.IMAGE;
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
                promptBuilder.append(", ");
            }
            promptBuilder.append(request.getUserInput());
        }
        
        return promptBuilder.toString();
    }
}


