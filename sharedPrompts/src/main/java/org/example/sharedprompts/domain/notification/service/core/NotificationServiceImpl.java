package org.example.sharedprompts.domain.notification.service.core;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.notification.service.core.strategy.NotificationQueryStrategy;
import org.example.sharedprompts.domain.notification.service.core.strategy.NotificationReadStrategy;
import org.example.sharedprompts.domain.notification.service.core.strategy.NotificationServiceFactory;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.service.UserLookupService;
import org.example.sharedprompts.dto.notification.response.NotificationResponseDto;
import org.example.sharedprompts.dto.notification.response.NotificationSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 서비스 구현체
 * 
 * 전략 패턴을 사용하여 조회 및 읽음 처리 로직을 분리하여 확장성을 제공합니다.
 * - 조회 전략: NotificationQueryStrategy (읽지 않은 알림만, 전체 알림 등)
 * - 읽음 처리 전략: NotificationReadStrategy (단일/전체 읽음 처리 등)
 * 
 * 책임 분리:
 * - User 조회: UserLookupService에 위임
 * - 조회 로직: NotificationQueryStrategy에 위임
 * - 읽음 처리: NotificationReadStrategy에 위임
 * - 전략 선택: NotificationServiceFactory에 위임
 * 
 * 새로운 조회 방식이나 읽음 처리 방식을 추가하려면 해당 전략 인터페이스를 구현하고
 * 팩토리에 등록하면 됩니다.
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final UserLookupService userLookupService;
    private final NotificationServiceFactory serviceFactory;

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getNotifications(Long userId, Pageable pageable) {
        User user = userLookupService.findById(userId);
        NotificationQueryStrategy queryStrategy = serviceFactory.getDefaultQueryStrategy();
        return queryStrategy.getNotifications(user, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationSummaryDto getUnreadCount(Long userId) {
        User user = userLookupService.findById(userId);
        NotificationQueryStrategy queryStrategy = serviceFactory.getDefaultQueryStrategy();
        return queryStrategy.getUnreadCount(user);
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        User user = userLookupService.findById(userId);
        NotificationReadStrategy readStrategy = serviceFactory.getDefaultReadStrategy();
        readStrategy.markAsRead(user, notificationId);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        User user = userLookupService.findById(userId);
        NotificationReadStrategy readStrategy = serviceFactory.getDefaultReadStrategy();
        readStrategy.markAllAsRead(user);
    }
}

