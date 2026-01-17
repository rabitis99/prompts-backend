package org.example.sharedprompts.domain.notification.setting.repository;

import org.example.sharedprompts.domain.notification.enums.NotificationType;
import org.example.sharedprompts.domain.notification.setting.UserNotificationSetting;
import org.example.sharedprompts.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserNotificationSettingRepository extends JpaRepository<UserNotificationSetting, Long> {

    /**
     * 사용자와 알림 타입으로 설정 조회
     */
    Optional<UserNotificationSetting> findByUserAndNotificationType(User user, NotificationType notificationType);

    /**
     * 사용자의 알림 타입별 수신 여부 확인
     */
    default boolean isEnabled(User user, NotificationType notificationType) {
        return findByUserAndNotificationType(user, notificationType)
                .map(UserNotificationSetting::isEnabled)
                .orElse(true); // 기본값: 수신 허용
    }
}

