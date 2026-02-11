package org.example.sharedprompts.module.domain.production.model.ai;

import lombok.Builder;
import lombok.Getter;
import org.example.sharedprompts.module.domain.production.service.ai.ContentType;

/**
 * AI 콘텐츠 생성 결과
 */
@Getter
@Builder
public class AIContentResult {
    private final boolean success;
    private final ContentType contentType;
    private final String content; // 생성된 원본 콘텐츠 (Text는 문자열, Image는 base64 또는 파일 경로)
    private final String modelName; // 사용된 AI 모델 이름
    private final String errorMessage;
    private final Long promptTokens; // 프롬프트 토큰 수
    private final Long completionTokens; // 생성된 콘텐츠 토큰 수
    private final Long totalTokens; // 총 토큰 수
    
    public static AIContentResult success(ContentType contentType, String content, String modelName) {
        return AIContentResult.builder()
                .success(true)
                .contentType(contentType)
                .content(content)
                .modelName(modelName)
                .build();
    }
    
    public static AIContentResult success(ContentType contentType, String content, String modelName,
                                         Long promptTokens, Long completionTokens, Long totalTokens) {
        return AIContentResult.builder()
                .success(true)
                .contentType(contentType)
                .content(content)
                .modelName(modelName)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(totalTokens)
                .build();
    }
    
    public static AIContentResult failure(String errorMessage) {
        return AIContentResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}


