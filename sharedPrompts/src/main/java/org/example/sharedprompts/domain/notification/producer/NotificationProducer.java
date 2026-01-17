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
                        // 지수 백오프: RETRY_DELAY_MS * 2^(attempt-1) = 1000ms, 2000ms, 4000ms...
                        long delayMs = RETRY_DELAY_MS * (1L << (attempt - 1));
                        TimeUnit.MILLISECONDS.sleep(delayMs);
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
        
        // 발행 실패한 메시지를 실패 큐에 보존 (DLQ는 consume 실패에만 작동하므로 별도 큐 사용)
        // 이렇게 하면 메시지가 손실되지 않고 나중에 재시도하거나 분석 가능
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_PUBLISH_FAILURE_EXCHANGE,
                    RabbitMQConfig.NOTIFICATION_PUBLISH_FAILURE_QUEUE,
                    message
            );
            log.info("Failed notification message preserved in failure queue: userId={}, type={}", 
                    message.getUserId(), message.getType());
        } catch (Exception failureQueueException) {
            // 실패 큐 발행도 실패하면 메시지 손실 위험 - 로그만 남기고 예외 전파
            log.error("Critical: Failed to publish to failure queue. Message will be lost: userId={}, type={}", 
                    message.getUserId(), message.getType(), failureQueueException);
        }
        
        // 예외를 던져서 호출자에게 알림
        // 메인 트랜잭션에 영향을 주지 않으려면 @Async를 사용하거나 별도 트랜잭션으로 처리
        throw new ApiException(ErrorCode.NOTIFICATION_PUBLISH_FAILED);
    }
}

