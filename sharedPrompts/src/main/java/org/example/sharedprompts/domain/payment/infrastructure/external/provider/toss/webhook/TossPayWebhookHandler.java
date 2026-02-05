package org.example.sharedprompts.domain.payment.infrastructure.external.provider.toss.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.webhook.PaymentWebhookHandler;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.webhook.WebhookEvent;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * TossPay Webhook Handler 구현체
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class TossPayWebhookHandler implements PaymentWebhookHandler {

    private final TossPayWebhookVerifier webhookVerifier;
    private final TossPayWebhookParser webhookParser;

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.TOSS;
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        if (payload == null || payload.isEmpty()) {
            log.warn("TossPay Webhook 검증 실패: payload가 비어있습니다");
            return false;
        }
        if (signature == null || signature.isEmpty()) {
            log.warn("TossPay Webhook 검증 실패: signature가 비어있습니다");
            return false;
        }

        boolean verified = webhookVerifier.verify(payload, signature);
        if (!verified) {
            log.warn("TossPay Webhook 서명 검증 실패");
        }
        return verified;
    }

    @Override
    public WebhookEvent parseWebhook(String payload) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("TossPay Webhook payload는 필수입니다");
        }

        try {
            WebhookEvent event = webhookParser.parse(payload);
            log.debug("TossPay Webhook 파싱 성공: eventType={}, externalPaymentId={}",
                    event.eventType(), 
                    event.externalPaymentId() != null ? SensitiveDataMasker.maskPaymentKey(event.externalPaymentId()) : null);
            return event;
        } catch (Exception e) {
            log.error("TossPay Webhook 파싱 실패: error={}", 
                    SensitiveDataMasker.maskSensitiveData(e.getMessage()), e);
            throw new RuntimeException("TossPay Webhook 파싱 실패: " + e.getMessage(), e);
        }
    }
}

