package org.example.sharedprompts.domain.notification.repository;

import org.example.sharedprompts.domain.notification.Notification;
import org.example.sharedprompts.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 사용자의 알림 목록 조회 (최신순)
     */
    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * 사용자의 읽지 않은 알림 개수 조회
     */
    long countByUserAndIsReadFalse(User user);

    /**
     * 사용자의 모든 알림을 읽음 처리
     */
    @Modifying(clearAutomatically = false)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user = :user AND n.isRead = false")
    int markAllAsReadByUser(@Param("user") User user);

    /**
     * 특정 알림을 읽음 처리
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.user = :user")
    int markAsReadByIdAndUser(@Param("id") Long id, @Param("user") User user);

    /**
     * 사용자의 읽지 않은 알림 목록 조회 (최신순)
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadNotificationsByUser(@Param("user") User user);

    /**
     * 최근 N분 내 같은 타입, 같은 관련 엔티티에 대한 알림 존재 여부 확인 (중복 방지)
     */
    @Query("SELECT COUNT(n) > 0 FROM Notification n WHERE n.user.id = :userId " +
           "AND n.type = :type AND n.relatedEntityId = :relatedEntityId " +
           "AND n.createdAt > :since")
    boolean existsRecentNotification(@Param("userId") Long userId,
                                     @Param("type") org.example.sharedprompts.domain.notification.enums.NotificationType type,
                                     @Param("relatedEntityId") Long relatedEntityId,
                                     @Param("since") java.time.LocalDateTime since);

    /**
     * 사용자의 읽은 알림 중 오래된 알림 삭제
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.user = :user AND n.isRead = true AND n.createdAt < :beforeDate")
    int deleteOldReadNotifications(@Param("user") User user, @Param("beforeDate") java.time.LocalDateTime beforeDate);

    /**
     * 모든 사용자의 읽은 알림 중 오래된 알림 삭제 (배치 삭제)
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.isRead = true AND n.createdAt < :beforeDate")
    int deleteAllOldReadNotifications(@Param("beforeDate") java.time.LocalDateTime beforeDate);
}


