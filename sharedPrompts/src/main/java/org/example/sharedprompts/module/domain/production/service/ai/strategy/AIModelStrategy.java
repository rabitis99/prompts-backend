package org.example.sharedprompts.module.domain.production.service.ai.strategy;

import org.example.sharedprompts.module.domain.production.service.ai.ContentType;

/**
 * AI 모델 전략 인터페이스
 * 다양한 AI 모델을 지원하기 위한 확장 포인트
 */
public interface AIModelStrategy {
    
    /**
     * 모델 이름 반환
     */
    String getModelName();
    
    /**
     * 기본 모델 여부
     */
    boolean isDefault();
    
    /**
     * 이 전략이 지원하는 콘텐츠 타입 반환
     * 
     * @return 지원하는 ContentType (TEXT 또는 IMAGE)
     */
    ContentType getSupportedContentType();
}

