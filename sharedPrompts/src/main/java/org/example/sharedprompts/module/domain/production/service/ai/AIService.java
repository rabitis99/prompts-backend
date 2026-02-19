package org.example.sharedprompts.module.domain.production.service.ai;

import org.example.sharedprompts.module.domain.production.model.ai.AIContentRequest;
import org.example.sharedprompts.module.domain.production.model.ai.AIContentResult;

/**
 * AI 콘텐츠 생성 서비스 인터페이스
 * Text, Image 생성만 담당하며, 여러 AI 모델을 지원할 수 있도록 확장 가능
 */
public interface AIService {
    
    /**
     * AI 콘텐츠 생성 요청 처리
     * 
     * @param request AI 생성 요청 정보
     * @return AI 생성 결과 (원본 콘텐츠)
     */
    AIContentResult generateContent(AIContentRequest request);
    
    /**
     * 지원하는 콘텐츠 타입 확인
     * 
     * @param contentType 콘텐츠 타입 (TEXT, IMAGE)
     * @return 지원 여부
     */
    boolean supports(ContentType contentType);
    
    /**
     * 이 서비스가 지원하는 ContentType을 직접 반환합니다.
     * 이 메서드를 구현하면 불필요한 반복을 제거하고 의도를 더 명확히 표현할 수 있습니다.
     * 
     * <p>기본 구현은 {@link #supports(ContentType)}를 사용하여 모든 ContentType을 순회합니다.
     * 구현체에서 이 메서드를 오버라이드하면 성능을 개선할 수 있습니다.
     * 
     * @return 이 서비스가 지원하는 ContentType, 지원하는 타입이 없으면 null
     */
    default ContentType getSupportedContentType() {
        for (ContentType type : ContentType.values()) {
            if (supports(type)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * AI 모델 이름 반환
     * 
     * @return 모델 이름 (예: "gpt-4", "dall-e-3", "claude-3")
     */
    String getModelName();
}


