package org.example.sharedprompts.domain.notification.service.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.notification.Notification;
import org.example.sharedprompts.domain.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 알림 배치 처리 서비스
 * - 여러 알림을 한 번에 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationBatchService {

    private final NotificationRepository notificationRepository;

    /**
     * 여러 알림을 배치로 저장
     * 
     * SSE 전송은 NotificationConsumer에서 RabbitMQ를 통해 비동기로 처리됨
     *
     * @param notifications 저장할 알림 목록
     * @return 저장된 알림 목록
     */
    @Transactional
    public List<Notification> saveBatch(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return List.of();
        }

        List<Notification> savedNotifications = notificationRepository.saveAll(notifications);
        log.debug("Batch saved {} notifications", savedNotifications.size());
        return savedNotifications;
    }

}

