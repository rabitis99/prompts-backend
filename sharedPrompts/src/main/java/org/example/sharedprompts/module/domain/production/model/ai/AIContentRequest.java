package org.example.sharedprompts.module.domain.production.model.ai;

import lombok.Builder;
import lombok.Getter;
import org.example.sharedprompts.module.domain.production.service.ai.ContentType;

/**
 * AI 콘텐츠 생성 요청
 */
@Getter
@Builder
public class AIContentRequest {
    private final ContentType contentType;
    private final String prompt; // 프롬프트 내용
    private final String userInput; // 사용자 입력값
    private final String modelName; // 사용할 AI 모델 이름 (선택적)
    
    // Image 생성 시 필요한 파라미터
    private final Integer width;
    private final Integer height;
    
    // Text 생성 시 필요한 파라미터
    private final String contentTypeHint; // "blog", "poem", "summary" 등
}