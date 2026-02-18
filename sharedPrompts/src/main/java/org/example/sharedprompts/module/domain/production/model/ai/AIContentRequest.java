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
    private final String prompt; // 프롬프트 내용 (PromptTemplateService에서 병합된 프롬프트)
    private final Long promptId; // 프롬프트 ID (참조용)
    private final Long userId; // 사용자 ID (권한 체크 및 저장용)
    private final String jobId; // Job ID (이미지 저장 시 사용)
    private final String userInput; // 사용자 입력값 (참조용, 이미 prompt에 병합됨)
    private final String modelName; // 사용할 AI 모델 이름 (선택적)
    
    // Image 생성 시 필요한 파라미터
    private final Integer width;
    private final Integer height;
    
    // Text 생성 시 필요한 파라미터
    private final String contentTypeHint; // "blog", "poem", "summary" 등
}