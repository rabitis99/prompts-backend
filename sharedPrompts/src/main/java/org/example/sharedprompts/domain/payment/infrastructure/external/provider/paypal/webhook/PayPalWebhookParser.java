package org.example.sharedprompts.domain.payment.infrastructure.external.provider.paypal.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.application.dto.response.PaymentResult;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.webhook.WebhookEvent;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.paypal.mapper.PayPalStatusMapper;
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
    public WebhookEvent parse(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("PayPal Webhook payload는 필수입니다");
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> webhookData = objectMapper.readValue(payload, Map.class);
            if (webhookData == null) {
                throw new IllegalStateException("PayPal Webhook payload가 비어있습니다");
            }

            String eventType = (String) webhookData.get("event_type");
            if (eventType == null || eventType.isBlank()) {
                throw new IllegalStateException("PayPal Webhook payload에 event_type이 없습니다");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> resource = (Map<String, Object>) webhookData.get("resource");
            if (resource == null) {
                throw new IllegalStateException("PayPal Webhook payload에 resource가 없습니다");
            }

            String orderId = extractOrderId(resource);
            Object rawId = resource.get("id");
            String captureId = rawId instanceof String ? (String) rawId : (rawId != null ? String.valueOf(rawId) : null);
            Object rawStatus = resource.get("status");
            String statusStr = rawStatus instanceof String ? (String) rawStatus : (rawStatus != null ? String.valueOf(rawStatus) : null);
            PaymentStatus status = statusMapper.map(statusStr);

            // orderId가 null이면 captureId를 대체값으로 사용
            String externalPaymentId = orderId != null ? orderId : captureId;
            if (externalPaymentId == null || externalPaymentId.isBlank()) {
                throw new IllegalStateException("PayPal Webhook payload에 orderId 또는 captureId가 없습니다");
            }

            PaymentResult paymentResult = PaymentResult.builder()
                    .externalPaymentId(externalPaymentId)
                    .status(status)
                    .metadata(objectMapper.writeValueAsString(resource))
                    .build();

            return new WebhookEvent(eventType, externalPaymentId, orderId != null ? orderId : externalPaymentId, paymentResult);
        } catch (JsonProcessingException e) {
            log.error("PayPal Webhook JSON 파싱 실패: error={}", e.getMessage(), e);
            throw new RuntimeException("PayPal Webhook JSON 파싱 실패: " + e.getMessage(), e);
        } catch (IllegalStateException e) {
            log.error("PayPal Webhook 페이로드 유효성 검사 실패: error={}", e.getMessage(), e);
            throw e;
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
                Object rawOrderId = relatedIds.get("order_id");
                return rawOrderId instanceof String ? (String) rawOrderId : (rawOrderId != null ? String.valueOf(rawOrderId) : null);
            }
        }
        return null;
    }
}

