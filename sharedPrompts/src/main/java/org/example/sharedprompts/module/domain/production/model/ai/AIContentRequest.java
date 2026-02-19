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

    /**
     * 프롬프트 내용 (PromptTemplateService에서 병합된 프롬프트)
     * 이 필드가 AI 호출에 실제로 사용되는 프롬프트입니다.
     */
    private final String prompt;

    /**
     * 프롬프트 ID (참조용)
     */
    private final Long promptId;

    /**
     * 사용자 ID (권한 체크 및 저장용)
     * <b>PII 주의:</b> 로깅 시 마스킹 처리됩니다.
     */
    private final Long userId;

    /**
     * Job ID (이미지 저장 시 사용)
     */
    private final String jobId;

    /**
     * 사용자 입력값 (참조용)
     * <b>주의:</b> 이 값은 이미 {@code prompt} 필드에 병합되어 있습니다.
     * AI 호출에는 {@code prompt} 필드만 사용되며, 이 필드는 디버깅 및 감사 목적으로만 보관됩니다.
     */
    private final String userInput;

    /**
     * 사용할 AI 모델 이름 (선택적)
     */
    private final String modelName;
    
    // Image 생성 시 필요한 파라미터
    private final Integer width;
    private final Integer height;
    
    // Text 생성 시 필요한 파라미터
    private final String contentTypeHint; // "blog", "poem", "summary" 등

    /**
     * 객체를 문자열로 변환합니다.
     * PII 보호를 위해 userId는 마스킹됩니다.
     *
     * @return 마스킹된 객체 정보
     */
    @Override
    public String toString() {
        return "AIContentRequest{" +
                "contentType=" + contentType +
                ", prompt=" + (prompt != null ? "[" + prompt.length() + " chars]" : "null") +
                ", promptId=" + promptId +
                ", userId=" + maskUserId(userId) +
                ", jobId=" + jobId +
                ", userInput=" + (userInput != null ? "[" + userInput.length() + " chars]" : "null") +
                ", modelName='" + modelName + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", contentTypeHint='" + contentTypeHint + '\'' +
                '}';
    }

    /**
     * userId를 마스킹합니다.
     *
     * @param userId 원본 사용자 ID
     * @return 마스킹된 사용자 ID (예: "12345" -> "***")
     */
    private String maskUserId(Long userId) {
        if (userId == null) {
            return "null";
        }
        return "***";
    }
}