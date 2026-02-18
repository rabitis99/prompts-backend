package org.example.sharedprompts.module.domain.production.service.ai.strategy;

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
}

