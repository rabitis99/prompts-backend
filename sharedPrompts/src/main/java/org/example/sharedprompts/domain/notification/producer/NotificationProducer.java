package org.example.sharedprompts.domain.notification.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.notification.message.NotificationMessage;
import org.example.sharedprompts.domain.notification.metrics.NotificationMetrics;
import org.example.sharedprompts.global.config.RabbitMQConfig;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 알림 메시지를 RabbitMQ로 발행하는 Producer
 * - 재시도 로직 포함
 * - 실패 시 예외를 던져서 Dead Letter Queue로 전달되도록 함
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationProducer {

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000L;

    private final RabbitTemplate rabbitTemplate;
    private final NotificationMetrics metrics;

    /**
     * 알림 메시지를 RabbitMQ로 발행
     * - 재시도 로직 포함 (최대 3회)
     * - 모든 재시도 실패 시 예외를 던져서 Dead Letter Queue로 전달
     *
     * @param message 알림 메시지
     * @throws ApiException 모든 재시도 실패 시
     */
    public void publishNotification(NotificationMessage message) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < MAX_RETRY_ATTEMPTS) {
            try {
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.NOTIFICATION_EXCHANGE,
                        RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
                        message
                );
                metrics.recordPublished(message.getType().name());
                log.debug("Notification message published to RabbitMQ: userId={}, type={}, attempt={}", 
                        message.getUserId(), message.getType(), attempt + 1);
                return; // 성공 시 즉시 반환
            } catch (Exception e) {
                lastException = e;
                attempt++;
                
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    log.warn("Failed to publish notification message (attempt {}/{}): userId={}, type={}, error={}", 
                            attempt, MAX_RETRY_ATTEMPTS, message.getUserId(), message.getType(), e.getMessage());
                    try {
                        TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS * attempt); // 지수 백오프
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Retry delay interrupted", ie);
                        break;
                    }
                }
            }
        }

        // 모든 재시도 실패
        metrics.recordFailed(message.getType().name(), "PUBLISH_FAILED");
        log.error("Failed to publish notification message after {} attempts: userId={}, type={}", 
                MAX_RETRY_ATTEMPTS, message.getUserId(), message.getType(), lastException);
        
        // 예외를 던져서 Dead Letter Queue로 전달되도록 함
        // 메인 트랜잭션에 영향을 주지 않으려면 @Async를 사용하거나 별도 트랜잭션으로 처리
        throw new ApiException(ErrorCode.NOTIFICATION_PUBLISH_FAILED);
    }
}

