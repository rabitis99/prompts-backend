package org.example.sharedprompts.domain.payment.service.event;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.event.PaymentEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

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
}

