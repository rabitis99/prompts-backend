package org.example.sharedprompts.domain.notification;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.domain.notification.enums.NotificationCategory;
import org.example.sharedprompts.domain.notification.enums.NotificationPriority;
import org.example.sharedprompts.domain.notification.enums.NotificationType;
import org.example.sharedprompts.domain.notification.enums.RelatedEntityType;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.global.entity.BaseEntity;

@Entity
@Getter
@Builder
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user_created", columnList = "user_id, created_at DESC"),
        @Index(name = "idx_notification_user_read", columnList = "user_id, is_read"),
        @Index(name = "idx_notification_user_read_created", columnList = "user_id, is_read, created_at DESC"),
        @Index(name = "idx_notification_type_related", columnList = "type, related_entity_id, created_at DESC"),
        // 추가된 인덱스 목록 (우선순위: 필수)
        // 그룹 알림 중복 방지 로직 최적화
        @Index(name = "idx_notification_group_key_read_created", columnList = "group_key, is_read, created_at DESC")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RelatedEntityType relatedEntityType; // 관련 엔티티 타입 (PROMPT, COMMENT)

    @Column(nullable = false)
    private Long relatedEntityId; // 프롬프트 ID 또는 댓글 ID

    @Column(nullable = false)
    private Long actorId; // 알림을 발생시킨 사용자 ID (댓글 작성자, 좋아요 누른 사람)

    @Column(nullable = false, length = 200)
    private String message; // 알림 메시지

    @Column(nullable = false)
    @Builder.Default
    private boolean isRead = false; // 읽음 여부

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NotificationPriority priority = NotificationPriority.NORMAL; // 우선순위

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NotificationCategory category = NotificationCategory.SOCIAL; // 카테고리

    @Column(length = 50)
    private String groupKey; // 그룹핑 키 (같은 그룹의 알림을 묶을 때 사용)

    @Column(nullable = false)
    @Builder.Default
    private Integer groupCount = 1; // 그룹 내 알림 개수

    public void markAsRead() {
        this.isRead = true;
    }

    public void markAsUnread() {
        this.isRead = false;
    }

    public void incrementGroupCount() {
        this.groupCount++;
    }
}

