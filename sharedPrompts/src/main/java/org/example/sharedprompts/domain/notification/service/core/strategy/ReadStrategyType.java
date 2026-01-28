package org.example.sharedprompts.domain.notification.service.core.strategy;

/**
 * 읽음 처리 전략 타입 열거형
 */
public enum ReadStrategyType {
    /**
     * 기본 전략: 단일/전체 읽음 처리
     */
    STANDARD,
    
    /**
     * 배치 읽음 처리 전략 (대량 알림 처리 최적화)
     */
    BATCH,
    
    /**
     * 스케줄링된 읽음 처리 전략 (지연 처리)
     */
    SCHEDULED
}

