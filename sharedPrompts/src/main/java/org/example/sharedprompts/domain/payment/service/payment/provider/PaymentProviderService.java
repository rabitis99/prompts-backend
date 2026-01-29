package org.example.sharedprompts.domain.payment.service.payment.provider;

import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;

import java.math.BigDecimal;

/**
 * 결제사별 결제 처리 서비스 인터페이스
 * Strategy 패턴 적용
 */
public interface PaymentProviderService {

    /**
 * Identifies the payment method handled by this service.
 *
 * @return the PaymentMethod this service handles
 */
    PaymentMethod getPaymentMethod();

    /**
 * Initiates an approval request for the given Payment with the provider.
 *
 * @param payment the payment to be approved, containing amount and payer details
 * @return the provider's external payment identifier or confirmation token
 */
    String approvePayment(Payment payment);

    /**
 * Retrieves the current status of a payment identified by the provider's external payment ID.
 *
 * @param externalPaymentId the payment identifier issued by the external payment provider
 * @return the current PaymentStatus of the referenced payment
 */
    PaymentStatus checkPaymentStatus(String externalPaymentId);

    /**
 * Cancels a payment identified by the external payment identifier.
 *
 * @param externalPaymentId the provider's external identifier for the payment to cancel
 * @param reason a short description of why the payment is being cancelled
 */
    void cancelPayment(String externalPaymentId, String reason);

    /**
 * Initiates a refund for a previously created external payment.
 *
 * @param externalPaymentId the external payment identifier to refund
 * @param amount the amount to refund (in the payment's currency)
 * @param reason a short description explaining the refund's purpose
 */
    void refundPayment(String externalPaymentId, BigDecimal amount, String reason);

    /**
 * Validates the authenticity of a webhook payload using its signature.
 *
 * @param payload   the raw webhook request body to verify
 * @param signature the signature provided with the webhook (e.g., header value)
 * @return          `true` if the signature is valid for the given payload, `false` otherwise
 */
    boolean verifyWebhookSignature(String payload, String signature);

    /**
 * Processes an incoming webhook payload and performs actions based on its content.
 *
 * @param payload the raw webhook request body to parse and handle
 */
    void processWebhook(String payload);
}
