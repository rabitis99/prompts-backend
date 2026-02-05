package org.example.sharedprompts.domain.payment.infrastructure.messaging.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.webhook.PaymentWebhookHandler;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.webhook.PaymentWebhookHandlerFactory;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.webhook.WebhookEvent;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookHandler {

    private final PaymentWebhookHandlerFactory webhookHandlerFactory;

    public WebhookEvent handle(PaymentMethod paymentMethod, String payload, String signature) {
        return handle(paymentMethod, payload, signature, null);
    }

    public WebhookEvent handle(
            PaymentMethod paymentMethod,
            String payload,
            String signature,
            Map<String, String> headers
    ) {
        PaymentWebhookHandler handler = webhookHandlerFactory.getHandler(paymentMethod);
        
        boolean verified = (headers != null && !headers.isEmpty())
                ? handler.verifyWebhookSignature(payload, headers)
                : handler.verifyWebhookSignature(payload, signature);

        if (!verified) {
            log.error("Webhook 서명 검증 실패: paymentMethod={}", paymentMethod);
            throw new ApiException(ErrorCode.PAYMENT_WEBHOOK_SIGNATURE_INVALID);
        }

        try {
            WebhookEvent event = handler.parseWebhook(payload);
            log.debug("Webhook 처리 성공: paymentMethod={}, eventType={}, externalPaymentId={}",
                    paymentMethod,
                    event.eventType(),
                    event.externalPaymentId());
            return event;
        } catch (RuntimeException e) {
            log.error("Webhook 파싱 실패: paymentMethod={}, error={}", paymentMethod, e.getMessage(), e);
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_RESPONSE_INVALID,
                    "Webhook payload 파싱 실패: " + e.getMessage());
        }
    }

    public boolean verifySignature(PaymentMethod paymentMethod, String payload, String signature) {
        return verifySignature(paymentMethod, payload, signature, null);
    }

    public boolean verifySignature(
            PaymentMethod paymentMethod,
            String payload,
            String signature,
            Map<String, String> headers
    ) {
        PaymentWebhookHandler handler = webhookHandlerFactory.getHandler(paymentMethod);
        
        return (headers != null && !headers.isEmpty())
                ? handler.verifyWebhookSignature(payload, headers)
                : handler.verifyWebhookSignature(payload, signature);
    }

    public WebhookEvent parse(PaymentMethod paymentMethod, String payload) {
        PaymentWebhookHandler handler = webhookHandlerFactory.getHandler(paymentMethod);
        
        try {
            return handler.parseWebhook(payload);
        } catch (RuntimeException e) {
            log.error("Webhook 파싱 실패: paymentMethod={}, error={}", paymentMethod, e.getMessage(), e);
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_RESPONSE_INVALID,
                    "Webhook payload 파싱 실패: " + e.getMessage());
        }
    }
}

