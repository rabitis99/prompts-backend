package org.example.sharedprompts.domain.payment.provider.paypal.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.provider.webhook.PaymentWebhookHandler;
import org.example.sharedprompts.domain.payment.provider.webhook.WebhookEvent;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * PayPal Webhook Handler 구현체
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class PayPalWebhookHandler implements PaymentWebhookHandler {

    private final PayPalWebhookVerifier webhookVerifier;
    private final PayPalWebhookParser webhookParser;

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.PAYPAL;
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        // PayPal은 서명 검증에 여러 헤더(PAYPAL-TRANSMISSION-*)가 필요하므로 headers 기반 메서드 사용을 권장합니다.
        // 하위 호환을 위해 기존 시그니처 기반 호출도 허용하되, signature를 JSON 형태로 전달해야 합니다.
        if (payload == null || payload.isBlank()) {
            log.warn("PayPal Webhook 검증 실패: payload가 비어있습니다");
            return false;
        }
        if (signature == null || signature.isBlank()) {
            log.warn("PayPal Webhook 검증 실패: signature가 비어있습니다");
            return false;
        }

        boolean verified = webhookVerifier.verify(payload, signature);
        if (!verified) {
            log.warn("PayPal Webhook 서명 검증 실패");
        }
        return verified;
    }

    @Override
    public boolean verifyWebhookSignature(String payload, Map<String, String> headers) {
        if (payload == null || payload.isBlank()) {
            log.warn("PayPal Webhook 검증 실패: payload가 비어있습니다");
            return false;
        }
        if (headers == null || headers.isEmpty()) {
            log.warn("PayPal Webhook 검증 실패: headers가 비어있습니다");
            return false;
        }

        boolean verified = webhookVerifier.verify(payload, headers);
        if (!verified) {
            log.warn("PayPal Webhook 서명 검증 실패");
        }
        return verified;
    }

    @Override
    public WebhookEvent parseWebhook(String payload) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("PayPal Webhook payload는 필수입니다");
        }

        try {
            WebhookEvent event = webhookParser.parse(payload);
            log.debug("PayPal Webhook 파싱 성공: eventType={}, externalPaymentId={}",
                    event.eventType(), 
                    event.externalPaymentId() != null ? SensitiveDataMasker.maskPaymentKey(event.externalPaymentId()) : null);
            return event;
        } catch (Exception e) {
            log.error("PayPal Webhook 파싱 실패: error={}", 
                    SensitiveDataMasker.maskSensitiveData(e.getMessage()), e);
            throw new RuntimeException("PayPal Webhook 파싱 실패: " + e.getMessage(), e);
        }
    }
}

