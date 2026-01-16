package org.example.sharedprompts.domain.notification.event;

import org.example.sharedprompts.domain.notification.Notification;

/**
 * 알림 저장 완료 이벤트
 * - 트랜잭션 커밋 후 SSE 전송을 위해 사용
 */
public record NotificationSavedEvent(
        Long userId,
        Notification notification
) {
    public static NotificationSavedEvent of(Long userId, Notification notification) {
        return new NotificationSavedEvent(userId, notification);
    }
}

