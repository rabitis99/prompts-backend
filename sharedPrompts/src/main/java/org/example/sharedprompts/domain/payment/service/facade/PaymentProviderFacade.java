package org.example.sharedprompts.domain.payment.service.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.logging.PaymentLoggingService;
import org.example.sharedprompts.domain.payment.service.payment.provider.PaymentProviderService;
import org.example.sharedprompts.domain.payment.service.payment.provider.PaymentProviderServiceFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 결제사 처리 파사드
 * 결제사별 승인, 취소, 환불, 상태 조회 로직을 담당
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProviderFacade {

    private final PaymentProviderServiceFactory providerServiceFactory;
    private final PaymentLoggingService loggingService;

    /**
     * Approves a payment using the specified actual charge amount.
     *
     * @param payment               the original Payment whose details (id, user, payment method, currency, metadata, etc.) are used to build the provider request
     * @param actualPaymentAmount   the amount to be charged after adjustments (for example, after point deductions)
     * @return                      the approval identifier returned by the payment provider
     */
    public String approvePayment(Payment payment, BigDecimal actualPaymentAmount) {
        PaymentProviderService providerService = providerServiceFactory.getService(payment.getPaymentMethod());
        
        loggingService.logPaymentApprovalAttempt(payment, payment.getPaymentMethod().name());

        // 실제 결제 금액으로 결제 승인 (포인트 사용 후 금액)
        Payment paymentForApproval = Payment.builder()
                .id(payment.getId())
                .user(payment.getUser())
                .amount(actualPaymentAmount) // 포인트 차감 후 실제 결제 금액
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .userType(payment.getUserType())
                .tier(payment.getTier())
                .status(payment.getStatus())
                .metadata(payment.getMetadata())
                .build();

        return providerService.approvePayment(paymentForApproval);
    }

    /**
     * Cancel the external payment associated with the given Payment if an external payment ID exists.
     *
     * If the payment has an external payment ID, the cancellation is delegated to the provider for the
     * payment's method; otherwise no action is taken.
     *
     * @param payment the payment containing (optionally) an external payment identifier
     * @param reason  the reason for cancellation to pass to the payment provider
     */
    public void cancelPayment(Payment payment, String reason) {
        if (payment.getExternalPaymentId() != null) {
            PaymentProviderService providerService = providerServiceFactory.getService(payment.getPaymentMethod());
            providerService.cancelPayment(payment.getExternalPaymentId(), reason);
        }
    }

    /**
     * Issue a refund with the payment provider for the given payment.
     *
     * If the payment does not have an external payment ID, no provider call is made.
     *
     * @param payment the payment to refund; the provider refund is performed only if `externalPaymentId` is present
     * @param refundAmount the amount to refund
     * @param reason a human-readable reason for the refund
     */
    public void refundPayment(Payment payment, BigDecimal refundAmount, String reason) {
        if (payment.getExternalPaymentId() != null) {
            PaymentProviderService providerService = providerServiceFactory.getService(payment.getPaymentMethod());
            providerService.refundPayment(payment.getExternalPaymentId(), refundAmount, reason);
        }
    }

    /**
     * Determine the current status of a payment, preferring the provider-reported status when available.
     *
     * @param payment the payment to check; if it has an externalPaymentId the provider will be queried
     * @return the provider-reported PaymentStatus when available and retrievable, otherwise the payment's current status
     */
    public PaymentStatus checkPaymentStatus(Payment payment) {
        if (payment.getExternalPaymentId() == null) {
            return payment.getStatus();
        }

        try {
            PaymentProviderService providerService = providerServiceFactory.getService(payment.getPaymentMethod());
            return providerService.checkPaymentStatus(payment.getExternalPaymentId());
        } catch (Exception e) {
            log.error("결제 상태 조회 실패: paymentId={}, error={}", payment.getId(), e.getMessage(), e);
            return payment.getStatus();
        }
    }
}
