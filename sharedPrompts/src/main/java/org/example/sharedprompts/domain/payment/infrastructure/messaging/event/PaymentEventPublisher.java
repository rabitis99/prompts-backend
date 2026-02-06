package org.example.sharedprompts.domain.payment.infrastructure.messaging.event;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.application.dto.response.CancelResult;
import org.example.sharedprompts.domain.payment.application.dto.response.PaymentResult;
import org.example.sharedprompts.domain.payment.application.dto.response.RefundResult;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 결제 이벤트 발행자
 */
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishPaymentFailed(Long paymentId, Long userId, String reason, String paymentMethod) {
        eventPublisher.publishEvent(new PaymentEvent.PaymentFailed(paymentId, userId, reason, paymentMethod));
    }

    public void publishPaymentSucceeded(Long paymentId, Long userId, String paymentMethod) {
        eventPublisher.publishEvent(new PaymentEvent.PaymentSucceeded(paymentId, userId, paymentMethod));
    }

    public void publishPaymentCanceled(Long paymentId, Long userId, String reason) {
        eventPublisher.publishEvent(new PaymentEvent.PaymentCanceled(paymentId, userId, reason));
    }

    public void publishPaymentRefunded(Long paymentId, Long userId, String reason) {
        eventPublisher.publishEvent(new PaymentEvent.PaymentRefunded(paymentId, userId, reason));
    }

    public void publishPaymentResultApplied(
            Long paymentId,
            PaymentResult result,
            BigDecimal actualAmount,
            String idempotencyKey
    ) {
        eventPublisher.publishEvent(new PaymentEvent.PaymentResultApplied(paymentId, result, actualAmount, idempotencyKey));
    }

    public void publishCancelResultApplied(
            Long paymentId,
            CancelResult result
    ) {
        eventPublisher.publishEvent(new PaymentEvent.CancelResultApplied(paymentId, result));
    }

    public void publishRefundResultApplied(
            Long paymentId,
            RefundResult result,
            BigDecimal refundAmount,
            String idempotencyKey
    ) {
        eventPublisher.publishEvent(new PaymentEvent.RefundResultApplied(paymentId, result, refundAmount, idempotencyKey));
    }
}

