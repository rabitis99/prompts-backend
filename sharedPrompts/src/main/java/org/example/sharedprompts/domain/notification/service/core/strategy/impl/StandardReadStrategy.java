package org.example.sharedprompts.domain.notification.service.core.strategy.impl;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.notification.repository.NotificationRepository;
import org.example.sharedprompts.domain.notification.service.core.strategy.NotificationReadStrategy;
import org.example.sharedprompts.domain.notification.service.core.strategy.ReadStrategyType;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기본 읽음 처리 전략 구현체
 * 단일 알림 및 전체 알림 읽음 처리를 담당합니다.
 */
@Component
@RequiredArgsConstructor
public class StandardReadStrategy implements NotificationReadStrategy {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    @CacheEvict(value = "unreadNotificationCount", key = "#user.id")
    public void markAsRead(User user, Long notificationId) {
        int updatedRows = notificationRepository.markAsReadByIdAndUser(notificationId, user);
        if (updatedRows == 0) {
            throw new ApiException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "unreadNotificationCount", key = "#user.id")
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsReadByUser(user);
    }

    @Override
    public ReadStrategyType getStrategyType() {
        return ReadStrategyType.STANDARD;
    }
}

