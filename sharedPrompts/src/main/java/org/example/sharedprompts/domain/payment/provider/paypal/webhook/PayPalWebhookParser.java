package org.example.sharedprompts.domain.payment.provider.paypal.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.model.PaymentResult;
import org.example.sharedprompts.domain.payment.provider.PaymentProvider;
import org.example.sharedprompts.domain.payment.provider.paypal.mapper.PayPalStatusMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * PayPal Webhook Parser
 * 
 * <p>단일 책임: Webhook payload 파싱만 담당
 * - 상태 변경 없음
 * - Null 안전성 보장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayPalWebhookParser {

    private final ObjectMapper objectMapper;
    private final PayPalStatusMapper statusMapper;

    /**
     * Webhook payload 파싱
     * 
     * @param payload Webhook 페이로드 (필수)
     * @return WebhookEvent
     * @throws IllegalArgumentException payload가 null이거나 비어있을 때
     * @throws RuntimeException 파싱 실패 시
     */
    public PaymentProvider.WebhookEvent parse(String payload) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("PayPal Webhook payload는 필수입니다");
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> webhookData = objectMapper.readValue(payload, Map.class);
            if (webhookData == null) {
                throw new RuntimeException("PayPal Webhook payload가 비어있습니다");
            }

            String eventType = (String) webhookData.get("event_type");
            if (eventType == null || eventType.isEmpty()) {
                throw new RuntimeException("PayPal Webhook payload에 event_type이 없습니다");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> resource = (Map<String, Object>) webhookData.get("resource");
            if (resource == null) {
                throw new RuntimeException("PayPal Webhook payload에 resource가 없습니다");
            }

            String orderId = extractOrderId(resource);
            String captureId = (String) resource.get("id");
            String statusStr = (String) resource.get("status");
            PaymentStatus status = statusMapper.map(statusStr);

            // orderId가 null이면 captureId를 대체값으로 사용
            String externalPaymentId = orderId != null ? orderId : captureId;
            if (externalPaymentId == null || externalPaymentId.isEmpty()) {
                throw new RuntimeException("PayPal Webhook payload에 orderId 또는 captureId가 없습니다");
            }

            PaymentResult paymentResult = PaymentResult.builder()
                    .externalPaymentId(externalPaymentId)
                    .status(status)
                    .metadata(objectMapper.writeValueAsString(resource))
                    .build();

            return new PaymentProvider.WebhookEvent(eventType, externalPaymentId, externalPaymentId, paymentResult);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("PayPal Webhook JSON 파싱 실패: error={}", e.getMessage(), e);
            throw new RuntimeException("PayPal Webhook JSON 파싱 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("PayPal Webhook 파싱 실패: error={}", e.getMessage(), e);
            throw new RuntimeException("PayPal Webhook 파싱 실패: " + e.getMessage(), e);
        }
    }

    private String extractOrderId(Map<String, Object> resource) {
        @SuppressWarnings("unchecked")
        Map<String, Object> supplementaryData = (Map<String, Object>) resource.get("supplementary_data");
        if (supplementaryData != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> relatedIds = (Map<String, Object>) supplementaryData.get("related_ids");
            if (relatedIds != null) {
                return (String) relatedIds.get("order_id");
            }
        }
        return null;
    }
}

