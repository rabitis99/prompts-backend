package org.example.sharedprompts.domain.notification.converter;

import org.example.sharedprompts.domain.notification.Notification;
import org.example.sharedprompts.domain.notification.message.NotificationMessage;
import org.example.sharedprompts.domain.user.User;
import org.springframework.stereotype.Component;

/**
 * Notification 관련 변환 로직 처리
 */
@Component
public class NotificationConverter {

    /**
     * NotificationMessage를 Notification 엔티티로 변환
     *
     * @param message 알림 메시지
     * @param user 알림을 받을 사용자 (프록시 가능, getReferenceById()로 생성 가능)
     * @return Notification 엔티티
     */
    public Notification toEntity(NotificationMessage message, User user) {
        return Notification.builder()
                .user(user)
                .type(message.getType())
                .relatedEntityType(message.getRelatedEntityType())
                .relatedEntityId(message.getRelatedEntityId())
                .actorId(message.getActorId())
                .message(message.getMessage())
                .isRead(false)
                .priority(message.getPriority())
                .category(message.getCategory())
                .groupKey(message.getGroupKey())
                .groupCount(1)
                .build();
    }
}

