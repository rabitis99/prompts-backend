package org.example.sharedprompts.domain.notification.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.notification.message.PromptCreatedMessage;
import org.example.sharedprompts.global.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 프롬프트 생성 SSE 알림 메시지를 RabbitMQ로 발행하는 Producer
 * - 재시도 로직 포함
 * - 실패 시 예외를 던져서 Dead Letter Queue로 전달되도록 함
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromptCreatedSseProducer {

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000L;

    private final RabbitTemplate rabbitTemplate;

    /**
     * 프롬프트 생성 SSE 알림 메시지를 RabbitMQ로 발행
     * - 재시도 로직 포함 (최대 3회)
     * - 모든 재시도 실패 시 예외를 던져서 Dead Letter Queue로 전달
     *
     * @param message 프롬프트 생성 SSE 알림 메시지
     * @throws ApiException 모든 재시도 실패 시
     */
    public void publishPromptCreated(PromptCreatedMessage message) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < MAX_RETRY_ATTEMPTS) {
            try {
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.SSE_EXCHANGE,
                        RabbitMQConfig.SSE_PROMPT_CREATED_ROUTING_KEY,
                        message
                );
                log.debug("Prompt created SSE message published to RabbitMQ: promptId={}, authorId={}, followerCount={}, attempt={}", 
                        message.getPromptId(), message.getAuthorId(), 
                        message.getFollowerIds() != null ? message.getFollowerIds().size() : 0, 
                        attempt + 1);
                return; // 성공 시 즉시 반환
            } catch (Exception e) {
                lastException = e;
                attempt++;
                
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    log.warn("Failed to publish prompt created SSE message (attempt {}/{}): promptId={}, error={}", 
                            attempt, MAX_RETRY_ATTEMPTS, message.getPromptId(), e.getMessage());
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
        log.error("Failed to publish prompt created SSE message after {} attempts: promptId={}, authorId={}", 
                MAX_RETRY_ATTEMPTS, message.getPromptId(), message.getAuthorId(), lastException);
        
        // SSE 알림 발행 실패는 프롬프트 생성 트랜잭션에 영향을 주지 않도록 예외를 던지지 않음
        // 실패한 메시지는 별도 큐에 저장하여 추후 재처리
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_PUBLISH_FAILURE_EXCHANGE,
                    RabbitMQConfig.NOTIFICATION_PUBLISH_FAILURE_QUEUE,
                    message
            );
            log.info("Failed prompt created SSE message preserved in failure queue: promptId={}, authorId={}", 
                    message.getPromptId(), message.getAuthorId());
        } catch (Exception retryEx) {
            log.error("Failed to send to failure queue: promptId={}, authorId={}", 
                    message.getPromptId(), message.getAuthorId(), retryEx);
        }
    }
}
