package org.example.sharedprompts.domain.notification.setting;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.domain.notification.enums.NotificationType;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.global.entity.BaseEntity;

/**
 * 사용자별 알림 설정
 * - 알림 타입별로 수신 여부를 관리
 */
@Entity
@Getter
@Builder
@Table(
        name = "user_notification_settings", 
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "notification_type"}),
        indexes = {
                // 추가된 인덱스 목록 (우선순위: 권장)
                // 사용자별 설정 조회 최적화
                @Index(name = "idx_user_notification_setting_user", columnList = "user_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserNotificationSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 20)
    private NotificationType notificationType;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true; // 알림 수신 여부

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }
}

