package org.example.sharedprompts.domain.notification.service.core.strategy;

import org.example.sharedprompts.domain.user.User;

/**
 * 알림 읽음 처리 전략 인터페이스
 * 
 * 다양한 읽음 처리 방식(단일 알림, 전체 알림, 배치 처리 등)을 
 * 전략 패턴으로 구현하여 확장성을 제공합니다.
 */
public interface NotificationReadStrategy {

    /**
     * 특정 알림을 읽음 처리
     * 
     * @param user 사용자
     * @param notificationId 알림 ID
     * @throws org.example.sharedprompts.global.exception.ApiException 알림을 찾을 수 없는 경우
     */
    void markAsRead(User user, Long notificationId);

    /**
     * 사용자의 모든 알림을 읽음 처리
     * 
     * @param user 사용자
     */
    void markAllAsRead(User user);

    /**
     * 전략 타입 반환
     * 
     * @return 전략 타입
     */
    ReadStrategyType getStrategyType();
}

