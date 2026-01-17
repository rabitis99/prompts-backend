package org.example.sharedprompts.scheduler.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.example.sharedprompts.domain.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 알림 정리 스케줄러
 * - 읽은 알림 중 오래된 알림을 주기적으로 삭제
 * - DB 용량 관리 및 성능 최적화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupScheduler {

    private final NotificationRepository notificationRepository;

    @Value("${notification.cleanup.enabled:true}")
    private boolean cleanupEnabled;

    @Value("${notification.cleanup.retention-days:90}")
    private int retentionDays;

    /**
     * 읽은 알림 중 오래된 알림 삭제
     * - 설정된 스케줄에 따라 실행
     */
    @Scheduled(cron = "${notification.cleanup.schedule:0 0 2 * * ?}")
    @SchedulerLock(
            name = "NotificationCleanupScheduler",
            lockAtMostFor = "30m",
            lockAtLeastFor = "5m"
    )
    @Transactional
    public void cleanupOldNotifications() {
        if (!cleanupEnabled) {
            log.debug("Notification cleanup is disabled");
            return;
        }

        log.info("NotificationCleanupScheduler started: retentionDays={}", retentionDays);

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        try {
            int deletedCount = notificationRepository.deleteAllOldReadNotifications(cutoffDate);
            log.info("NotificationCleanupScheduler finished: deleted {} old read notifications", deletedCount);
        } catch (Exception e) {
            log.error("NotificationCleanupScheduler failed: cutoffDate={}", cutoffDate, e);
            throw e;
        }
    }
}

