package org.example.sharedprompts.domain.payment.infrastructure.external.provider.webhook;

import org.example.sharedprompts.domain.payment.application.dto.response.PaymentResult;

/**
 * 파싱된 Webhook 이벤트
 */
public record WebhookEvent(
        String eventType,
        String externalPaymentId,
        String orderId,
        PaymentResult paymentResult
) {}
