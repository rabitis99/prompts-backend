package org.example.sharedprompts.domain.notification.service.core.strategy.impl;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.notification.repository.NotificationRepository;
import org.example.sharedprompts.domain.notification.service.core.strategy.NotificationQueryStrategy;
import org.example.sharedprompts.domain.notification.service.core.strategy.QueryStrategyType;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.dto.notification.response.NotificationResponseDto;
import org.example.sharedprompts.dto.notification.response.NotificationSummaryDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 읽지 않은 알림만 조회하는 기본 전략 구현체
 */
@Component
@RequiredArgsConstructor
public class UnreadOnlyQueryStrategy implements NotificationQueryStrategy {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getNotifications(User user, Pageable pageable) {
        return notificationRepository
                .findByUserAndIsReadFalseOrderByCreatedAtDesc(user, pageable)
                .map(NotificationResponseDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "unreadNotificationCount", key = "#user.id", unless = "#result.unreadCount == 0")
    public NotificationSummaryDto getUnreadCount(User user) {
        long unreadCount = notificationRepository.countByUserAndIsReadFalse(user);
        return new NotificationSummaryDto(unreadCount);
    }

    @Override
    public QueryStrategyType getStrategyType() {
        return QueryStrategyType.UNREAD_ONLY;
    }
}

