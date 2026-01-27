package org.example.sharedprompts.domain.notification.service.core;

import org.example.sharedprompts.dto.notification.response.NotificationResponseDto;
import org.example.sharedprompts.dto.notification.response.NotificationSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    /**
     * 사용자의 알림 목록 조회 (페이징)
     */
    Page<NotificationResponseDto> getNotifications(Long userId, Pageable pageable);

    /**
     * 사용자의 읽지 않은 알림 개수 조회
     */
    NotificationSummaryDto getUnreadCount(Long userId);

    /**
     * 특정 알림을 읽음 처리
     */
    void markAsRead(Long userId, Long notificationId);

    /**
     * 사용자의 모든 알림을 읽음 처리
     */
    void markAllAsRead(Long userId);
}

