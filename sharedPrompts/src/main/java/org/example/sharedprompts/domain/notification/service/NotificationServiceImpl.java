package org.example.sharedprompts.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.notification.Notification;
import org.example.sharedprompts.domain.notification.repository.NotificationRepository;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.notification.response.NotificationResponseDto;
import org.example.sharedprompts.dto.notification.response.NotificationSummaryDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getNotifications(Long userId, Pageable pageable) {
        User user = getUser(userId);
        Page<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        return notifications.map(NotificationResponseDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "unreadNotificationCount", key = "#userId", unless = "#result.unreadCount == 0")
    public NotificationSummaryDto getUnreadCount(Long userId) {
        User user = getUser(userId);
        long unreadCount = notificationRepository.countByUserAndIsReadFalse(user);
        return new NotificationSummaryDto(unreadCount);
    }

    @Override
    @Transactional
    @CacheEvict(value = "unreadNotificationCount", key = "#userId")
    public void markAsRead(Long userId, Long notificationId) {
        User user = getUser(userId);
        int updatedRows = notificationRepository.markAsReadByIdAndUser(notificationId, user);
        if (updatedRows == 0) {
            throw new ApiException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "unreadNotificationCount", key = "#userId")
    public void markAllAsRead(Long userId) {
        User user = getUser(userId);
        notificationRepository.markAllAsReadByUser(user);
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }
}

