package org.example.sharedprompts.domain.notification.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.notification.metrics.NotificationMetrics;
import org.example.sharedprompts.global.config.RabbitMQConfig;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 프롬프트 생성 SSE 알림 Dead Letter Queue Consumer
 * - 실패한 메시지를 처리하고 모니터링
 * - 로깅 및 메트릭 기록
 * - 필요시 알림 발송 가능
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromptCreatedSseDlqConsumer {

    private final NotificationMetrics metrics;

    /**
     * DLQ에서 실패한 메시지를 수신하여 처리
     * - 실패한 메시지 정보를 로깅
     * - 메트릭 기록
     * - 운영팀 알림 필요 시 추가 가능
     *
     * @param message 실패한 메시지
     */
    @RabbitListener(queues = RabbitMQConfig.SSE_DLQ)
    public void handleDlqMessage(Message message) {
        try {
            var headers = message.getMessageProperties().getHeaders();
            
            // 메시지 헤더에서 정보 추출
            String routingKey = (String) headers.get("x-original-routing-key");
            String exchange = (String) headers.get("x-first-death-exchange");
            String queue = (String) headers.get("x-first-death-queue");
            String reason = (String) headers.get("x-first-death-reason");
            
            // x-death는 리스트 형태로 제공됨
            var deaths = headers.get("x-death");
            int deathCount = deaths != null ? 1 : 0; // 간단한 카운트
            
            // 메시지 본문 추출
            String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
            
            log.error("Received failed message in DLQ: " +
                    "routingKey={}, exchange={}, queue={}, reason={}, deathCount={}, messageBody={}", 
                    routingKey, exchange, queue, reason, deathCount, messageBody);

            // DLQ 메시지 카운터 증가
            metrics.recordDlqMessage("SSE_PROMPT_CREATED");

            // TODO: 운영팀에게 알림 발송 (이메일, 슬랙 등)
            // alertService.sendAlert("SSE 알림 메시지 처리 실패", messageBody);

        } catch (Exception e) {
            log.error("Failed to process DLQ message", e);
            // DLQ 처리 실패도 메트릭 기록
            metrics.recordDlqMessage("SSE_PROMPT_CREATED");
        }
    }
}

