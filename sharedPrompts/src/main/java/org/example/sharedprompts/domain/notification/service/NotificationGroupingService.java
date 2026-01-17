package org.example.sharedprompts.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.notification.Notification;
import org.example.sharedprompts.domain.notification.enums.NotificationType;
import org.example.sharedprompts.domain.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 알림 그룹핑 서비스
 * - 같은 그룹의 알림을 묶어서 처리
 * - 예: 같은 프롬프트에 여러 댓글이 달리면 하나의 알림으로 그룹핑
 */
@Service
@RequiredArgsConstructor
public class NotificationGroupingService {

    private final NotificationRepository notificationRepository;

    /**
     * 그룹 키 생성
     * - 같은 관련 엔티티에 대한 같은 타입의 알림을 그룹핑
     */
    public String generateGroupKey(Long userId, NotificationType type, Long relatedEntityId) {
        return String.format("%d:%s:%d", userId, type.name(), relatedEntityId);
    }

    /**
     * 기존 그룹 알림 찾기
     * - 최근 일정 시간 내 같은 그룹의 읽지 않은 알림이 있는지 확인
     */
    @Transactional(readOnly = true)
    public Optional<Notification> findExistingGroupNotification(String groupKey, int windowMinutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(windowMinutes);
        
        return notificationRepository.findAll().stream()
                .filter(n -> groupKey.equals(n.getGroupKey()))
                .filter(n -> !n.isRead())
                .filter(n -> n.getCreatedAt().isAfter(since))
                .findFirst();
    }

    /**
     * 그룹 알림 업데이트
     * - 기존 알림의 그룹 카운트를 증가
     */
    @Transactional
    public void updateGroupNotification(Notification notification) {
        notification.incrementGroupCount();
        notificationRepository.save(notification);
    }
}

