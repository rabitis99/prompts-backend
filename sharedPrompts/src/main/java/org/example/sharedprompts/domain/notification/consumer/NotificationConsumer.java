package org.example.sharedprompts.domain.notification.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.notification.converter.NotificationConverter;
import org.example.sharedprompts.domain.notification.metrics.NotificationMetrics;
import org.example.sharedprompts.domain.notification.Notification;
import org.example.sharedprompts.domain.notification.message.NotificationMessage;
import org.example.sharedprompts.domain.notification.repository.NotificationRepository;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.domain.notification.event.NotificationSavedEvent;
import org.example.sharedprompts.global.config.RabbitMQConfig;
import org.example.sharedprompts.global.exception.ApiException;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.Valid;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * RabbitMQ에서 알림 메시지를 수신하여 처리하는 Consumer
 * - 알림 저장
 * - SSE를 통한 실시간 알림 전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationConverter notificationConverter;
    private final NotificationMetrics metrics;
    private final ApplicationEventPublisher eventPublisher;

    // ======================
    //      내부 헬퍼 메서드
    // ======================

    /**
     * 사용자 조회
     * - 최적화: getReferenceById()를 사용하여 프록시만 가져옴 (DB 조회 없음)
     * - 실제 사용자 정보가 필요할 때만 DB 조회 발생
     */
    private User getUser(Long userId) {
        return userRepository.getReferenceById(userId);
    }

    /**
     * RabbitMQ에서 알림 메시지를 수신하여 처리
     * - 실패 시 자동으로 Dead Letter Queue로 전달됨
     *
     * @param message 알림 메시지
     */
    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    @Transactional
    public void handleNotification(@Valid NotificationMessage message) {
        Timer.Sample sample = metrics.startProcessingTimer();
        String type = message.getType().name();

        try {
            log.debug("Received notification message from RabbitMQ: userId={}, type={}", 
                    message.getUserId(), type);

            // 사용자 조회
            User user = getUser(message.getUserId());

            // 알림 생성 및 저장
            Notification notification = notificationConverter.toEntity(message, user);
            Notification savedNotification = notificationRepository.save(notification);
            log.debug("Notification saved: id={}, userId={}", savedNotification.getId(), message.getUserId());

            // 트랜잭션 커밋 후 SSE 전송을 위해 이벤트 발행
            eventPublisher.publishEvent(NotificationSavedEvent.of(message.getUserId(), savedNotification));

            // 메트릭 기록
            metrics.recordProcessed(type);
            metrics.recordProcessingTime(sample, type, "success");

        } catch (ApiException e) {
            metrics.recordFailed(type, e.getErrorCode().getCode());
            metrics.recordProcessingTime(sample, type, "failed");
            throw e;
        } catch (Exception e) {
            metrics.recordFailed(type, "UNKNOWN");
            metrics.recordProcessingTime(sample, type, "failed");
            throw e;
        }
    }

}

