package org.example.sharedprompts.domain.payment.infrastructure.external.provider.kakao.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.webhook.PaymentWebhookHandler;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.webhook.WebhookEvent;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * KakaoPay Webhook Handler 구현체
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class KakaoPayWebhookHandler implements PaymentWebhookHandler {

    private final KakaoPayWebhookVerifier webhookVerifier;
    private final KakaoPayWebhookParser webhookParser;

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.KAKAO_PAY;
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        if (payload == null || payload.isEmpty() || signature == null || signature.isEmpty()) {
            log.warn("KakaoPay Webhook 검증 실패: payload 또는 signature가 비어있습니다");
            return false;
        }
        boolean verified = webhookVerifier.verify(payload, signature);
        if (!verified) {
            log.warn("KakaoPay Webhook 서명 검증 실패");
        }
        return verified;
    }

    @Override
    public WebhookEvent parseWebhook(String payload) {
        try {
            WebhookEvent event = webhookParser.parse(payload);
            log.debug("KakaoPay Webhook 파싱 성공: eventType={}, externalPaymentId={}",
                    event.eventType(), 
                    event.externalPaymentId() != null ? SensitiveDataMasker.maskPaymentKey(event.externalPaymentId()) : null);
            return event;
        } catch (Exception e) {
            log.error("KakaoPay Webhook 파싱 실패: error={}", 
                    SensitiveDataMasker.maskSensitiveData(e.getMessage()), e);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("KakaoPay Webhook 파싱 실패: " + e.getMessage(), e);
        }
    }
}

