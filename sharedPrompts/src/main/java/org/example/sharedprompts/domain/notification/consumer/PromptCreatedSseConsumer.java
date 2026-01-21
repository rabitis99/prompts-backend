package org.example.sharedprompts.domain.notification.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.notification.factory.PromptCreatedSsePayloadFactory;
import org.example.sharedprompts.domain.notification.message.PromptCreatedMessage;
import org.example.sharedprompts.domain.notification.metrics.NotificationMetrics;
import org.example.sharedprompts.domain.notification.service.SseBatchSender;
import org.example.sharedprompts.domain.notification.validator.PromptCreatedMessageValidator;
import org.example.sharedprompts.global.config.RabbitMQConfig;
import jakarta.validation.Valid;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ에서 프롬프트 생성 SSE 알림 메시지를 수신하여 처리하는 Consumer
 * - 프롬프트 생성 시 팔로워들에게 SSE 알림 전송
 * - 다중 인스턴스 환경에서 각 인스턴스가 자신에게 연결된 사용자에게만 SSE 전송
 * - 처리 실패 시 DLQ로 전달 (예외 발생)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromptCreatedSseConsumer {

    private static final String MESSAGE_TYPE = "SSE_PROMPT_CREATED";

    private final PromptCreatedSsePayloadFactory payloadFactory;
    private final NotificationMetrics metrics;
    private final SseBatchSender sseBatchSender;
    private final PromptCreatedMessageValidator messageValidator;

    /**
     * RabbitMQ에서 프롬프트 생성 SSE 알림 메시지를 수신하여 처리
     * - 각 팔로워에게 SSE 알림 전송
     * - 로컬 인스턴스에 연결되지 않은 사용자는 자동으로 무시됨 (다른 인스턴스가 처리)
     * - 모든 팔로워에게 전송 실패 시 예외를 던져서 DLQ로 전달
     *
     * @param message 프롬프트 생성 SSE 알림 메시지
     */
    @RabbitListener(queues = RabbitMQConfig.SSE_PROMPT_CREATED_QUEUE)
    public void handlePromptCreated(@Valid PromptCreatedMessage message) {
        // 메시지 검증 (null 체크 포함)
        if (!messageValidator.shouldSend(message)) {
            log.debug("Message validation failed or no followers to notify: promptId={}", 
                    message != null ? message.getPromptId() : null);
            return;
        }

        // 검증 통과 후 로깅 (null 안전성 보장됨)
        log.debug("Received prompt created SSE message from RabbitMQ: promptId={}, authorId={}, followerCount={}", 
                message.getPromptId(), message.getAuthorId(), 
                message.getFollowerIds() != null ? message.getFollowerIds().size() : 0);

        // SSE 페이로드 생성
        var payload = payloadFactory.createPayload(message);

        // 배치 전송 실행
        var result = sseBatchSender.sendBatch(message.getFollowerIds(), payload);

        // 메트릭 기록
        recordMetrics(result);

        // 모든 전송 실패 시 DLQ로 전달
        if (result.isAllFailed()) {
            metrics.recordFailed(MESSAGE_TYPE, "ALL_SSE_SEND_FAILED");
            log.error("All SSE notifications failed for prompt: promptId={}, authorId={}, followerCount={}", 
                    message.getPromptId(), message.getAuthorId(), message.getFollowerIds().size(), 
                    result.getLastException());
            throw new RuntimeException(
                    "All SSE notifications failed for prompt: " + message.getPromptId(), 
                    result.getLastException());
        }

        log.debug("Prompt created SSE notification completed: promptId={}, successCount={}, failureCount={}", 
                message.getPromptId(), result.getSuccessCount(), result.getFailureCount());
    }

    /**
     * 전송 결과에 따른 메트릭 기록
     */
    private void recordMetrics(SseBatchSender.SseSendResult result) {
        if (result.getSuccessCount() > 0) {
            metrics.recordSseSent(MESSAGE_TYPE);
        }
        
        // 실패한 경우 각 실패마다 메트릭 기록
        for (int i = 0; i < result.getFailureCount(); i++) {
            metrics.recordSseFailed(MESSAGE_TYPE);
        }
    }
}
