package org.example.sharedprompts.domain.notification.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.audit.auth.util.AuthHashUtil;
import org.example.sharedprompts.domain.notification.metrics.NotificationMetrics;
import org.example.sharedprompts.infra.messaging.RabbitMQConfig;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

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
    private final AuthHashUtil authHashUtil;

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
            
            // 메시지 본문 정보 추출 (개인정보 보호를 위해 본문 전체는 로그에 남기지 않음)
            byte[] messageBody = message.getBody();
            int messageSize = messageBody != null ? messageBody.length : 0;
            // AuthHashUtil을 사용하여 해시 계산 (byte[] 직접 해시 - 비UTF-8 payload 안전, 메모리 효율적)
            String messageHash = authHashUtil.hash(messageBody);
            if (messageHash == null) {
                messageHash = "empty_or_invalid_message";
            }
            
            log.error("Received failed message in DLQ: " +
                    "routingKey={}, exchange={}, queue={}, reason={}, deathCount={}, messageSize={}, messageHash={}", 
                    routingKey, exchange, queue, reason, deathCount, messageSize, messageHash);

            // DLQ 메시지 카운터 증가
            metrics.recordDlqMessage("SSE_PROMPT_CREATED");

            // 운영팀 알림 발송이 필요한 경우:
            // 1. 메트릭 기반 알림: Prometheus AlertManager를 통해 DLQ 메시지 수가 임계값을 초과하면 알림 발송
            // 2. 직접 알림: 이메일/Slack 연동 서비스를 주입받아 여기서 직접 호출
            // 3. 이벤트 발행: DLQ 메시지 수신 이벤트를 발행하여 별도 핸들러에서 처리
            // 현재는 로그와 메트릭으로 모니터링하며, 필요 시 위 방법 중 하나를 선택하여 구현

        } catch (Exception e) {
            log.error("Failed to process DLQ message", e);
            // DLQ 처리 실패도 메트릭 기록
            metrics.recordDlqMessage("SSE_PROMPT_CREATED");
        }
    }

}

