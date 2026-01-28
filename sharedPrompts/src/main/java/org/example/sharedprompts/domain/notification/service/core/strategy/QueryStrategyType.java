package org.example.sharedprompts.domain.notification.service.core.strategy;

/**
 * 조회 전략 타입 열거형
 */
public enum QueryStrategyType {
    /**
     * 기본 전략: 읽지 않은 알림만 조회
     */
    UNREAD_ONLY,
    
    /**
     * 전체 알림 조회 전략 (읽은/읽지 않은 모두)
     */
    ALL,
    
    /**
     * 필터링된 알림 조회 전략 (타입별, 카테고리별 등)
     */
    FILTERED
}

