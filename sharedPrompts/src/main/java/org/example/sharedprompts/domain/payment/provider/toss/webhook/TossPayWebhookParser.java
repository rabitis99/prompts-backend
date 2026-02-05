package org.example.sharedprompts.domain.payment.provider.toss.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.model.PaymentResult;
import org.example.sharedprompts.domain.payment.provider.webhook.PaymentWebhookHandler;
import org.example.sharedprompts.domain.payment.provider.webhook.WebhookEvent;
import org.example.sharedprompts.domain.payment.provider.toss.mapper.TossPayStatusMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * TossPay Webhook Parser
 * 
 * <p>단일 책임: Webhook payload 파싱만 담당
 * - 상태 변경 없음
 * - Null 안전성 보장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TossPayWebhookParser {

    private final ObjectMapper objectMapper;
    private final TossPayStatusMapper statusMapper;

    /**
     * Webhook payload 파싱
     */
    public WebhookEvent parse(String payload) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("TossPay Webhook payload는 필수입니다");
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> webhookData = objectMapper.readValue(payload, Map.class);
            if (webhookData == null) {
                throw new RuntimeException("TossPay Webhook payload가 비어있습니다");
            }

            String eventType = (String) webhookData.get("event");
            if (eventType == null || eventType.isEmpty()) {
                throw new RuntimeException("TossPay Webhook payload에 event가 없습니다");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) webhookData.get("data");
            if (data == null) {
                throw new RuntimeException("TossPay Webhook payload에 data가 없습니다");
            }

            String paymentKey = (String) data.get("paymentKey");
            if (paymentKey == null || paymentKey.isEmpty()) {
                throw new RuntimeException("TossPay Webhook payload에 paymentKey가 없습니다");
            }

            String orderId = (String) data.get("orderId");
            if (orderId == null || orderId.isEmpty()) {
                throw new RuntimeException("TossPay Webhook payload에 orderId가 없습니다");
            }

            String statusStr = (String) data.get("status");
            PaymentStatus status = statusMapper.map(statusStr);

            PaymentResult paymentResult = PaymentResult.builder()
                    .externalPaymentId(paymentKey)
                    .status(status)
                    .orderId(orderId)
                    .metadata(objectMapper.writeValueAsString(data))
                    .build();

            return new WebhookEvent(eventType, paymentKey, orderId, paymentResult);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("TossPay Webhook JSON 파싱 실패: error={}", e.getMessage(), e);
            throw new RuntimeException("TossPay Webhook JSON 파싱 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("TossPay Webhook 파싱 실패: error={}", e.getMessage(), e);
            throw new RuntimeException("TossPay Webhook 파싱 실패: " + e.getMessage(), e);
        }
    }
}

