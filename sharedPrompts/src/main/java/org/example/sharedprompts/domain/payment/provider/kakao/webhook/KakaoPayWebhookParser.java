package org.example.sharedprompts.domain.payment.provider.kakao.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.model.PaymentResult;
import org.example.sharedprompts.domain.payment.provider.webhook.PaymentWebhookHandler;
import org.example.sharedprompts.domain.payment.provider.webhook.WebhookEvent;
import org.example.sharedprompts.domain.payment.provider.kakao.mapper.KakaoPayStatusMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * KakaoPay Webhook Parser
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoPayWebhookParser {

    private final ObjectMapper objectMapper;
    private final KakaoPayStatusMapper statusMapper;

    public WebhookEvent parse(String payload) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("KakaoPay Webhook payload는 필수입니다");
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = objectMapper.readValue(payload, Map.class);
            if (body == null) {
                throw new RuntimeException("KakaoPay Webhook payload가 비어있습니다");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) body.get("data");
            if (data == null) {
                throw new RuntimeException("KakaoPay Webhook payload에 data가 없습니다");
            }

            String eventType = (String) body.get("event");
            if (eventType == null || eventType.isEmpty()) {
                throw new RuntimeException("KakaoPay Webhook payload에 event가 없습니다");
            }

            String tid = (String) data.get("tid");
            if (tid == null || tid.isEmpty()) {
                throw new RuntimeException("KakaoPay Webhook payload에 tid가 없습니다");
            }

            String orderId = (String) data.get("partner_order_id");
            if (orderId == null || orderId.isEmpty()) {
                throw new RuntimeException("KakaoPay Webhook payload에 partner_order_id가 없습니다");
            }

            String statusStr = (String) data.get("status");
            PaymentStatus status = statusMapper.map(statusStr);

            PaymentResult result = PaymentResult.builder()
                    .externalPaymentId(tid)
                    .orderId(orderId)
                    .status(status)
                    .metadata(objectMapper.writeValueAsString(data))
                    .build();

            return new WebhookEvent(eventType, tid, orderId, result);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("KakaoPay Webhook JSON 파싱 실패: error={}", e.getMessage(), e);
            throw new RuntimeException("KakaoPay Webhook JSON 파싱 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("KakaoPay Webhook 파싱 실패: error={}", e.getMessage(), e);
            throw new RuntimeException("KakaoPay Webhook 파싱 실패: " + e.getMessage(), e);
        }
    }
}
