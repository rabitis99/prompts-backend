package org.example.sharedprompts.domain.payment.provider.webhook;

import org.example.sharedprompts.domain.payment.model.PaymentResult;

/**
 * 파싱된 Webhook 이벤트
 */
public record WebhookEvent(
        String eventType,
        String externalPaymentId,
        String orderId,
        PaymentResult paymentResult
) {}
