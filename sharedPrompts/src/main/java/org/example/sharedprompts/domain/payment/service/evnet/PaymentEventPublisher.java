package org.example.sharedprompts.domain.payment.service.evnet;

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

    /**
     * Publish a PaymentFailed event for the specified payment and user.
     *
     * @param paymentId     the identifier of the payment that failed
     * @param userId        the identifier of the user associated with the payment
     * @param reason        a brief reason or description of why the payment failed
     * @param paymentMethod the payment method used for the payment
     */
    public void publishPaymentFailed(Long paymentId, Long userId, String reason, String paymentMethod) {
        eventPublisher.publishEvent(new PaymentEvent.PaymentFailed(paymentId, userId, reason, paymentMethod));
    }

    /**
     * Publish a PaymentSucceeded event to the application event stream.
     *
     * @param paymentId     the identifier of the payment
     * @param userId        the identifier of the user who made the payment
     * @param paymentMethod the payment method used (e.g., "credit_card", "paypal")
     */
    public void publishPaymentSucceeded(Long paymentId, Long userId, String paymentMethod) {
        eventPublisher.publishEvent(new PaymentEvent.PaymentSucceeded(paymentId, userId, paymentMethod));
    }

    /**
     * Publish a payment-canceled event containing the payment and user identifiers and a cancellation reason.
     *
     * @param paymentId the identifier of the payment that was canceled
     * @param userId    the identifier of the user associated with the payment
     * @param reason    a short description of why the payment was canceled
     */
    public void publishPaymentCanceled(Long paymentId, Long userId, String reason) {
        eventPublisher.publishEvent(new PaymentEvent.PaymentCanceled(paymentId, userId, reason));
    }

    /**
     * Publishes a PaymentRefunded event with the given payment ID, user ID, and refund reason.
     *
     * @param paymentId the identifier of the refunded payment
     * @param userId the identifier of the user associated with the payment
     * @param reason a short description of why the payment was refunded
     */
    public void publishPaymentRefunded(Long paymentId, Long userId, String reason) {
        eventPublisher.publishEvent(new PaymentEvent.PaymentRefunded(paymentId, userId, reason));
    }
}
