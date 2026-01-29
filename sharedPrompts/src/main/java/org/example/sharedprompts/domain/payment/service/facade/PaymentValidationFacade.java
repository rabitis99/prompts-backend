package org.example.sharedprompts.domain.payment.service.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.enums.UserTier;
import org.example.sharedprompts.domain.payment.logging.PaymentLoggingService;
import org.example.sharedprompts.domain.payment.metrics.PaymentMetrics;
import org.example.sharedprompts.domain.payment.repository.PaymentRepository;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 결제 검증 파사드
 * 티어 체크, 권한 체크, 상태 체크 등의 검증 로직을 담당
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentValidationFacade {

    private final PaymentRepository paymentRepository;
    private final PaymentLoggingService loggingService;
    private final PaymentMetrics paymentMetrics;

    /**
     * Enforces the user's daily successful payment limit based on their tier.
     *
     * @param userId the identifier of the user to validate
     * @param tier the user's tier which provides the allowed daily limit
     * @throws ApiException if the number of today's successful payments for the user is greater than or equal to the tier's daily limit (ErrorCode.PAYMENT_DAILY_LIMIT_EXCEEDED)
     */
    public void validateDailyLimit(Long userId, UserTier tier) {
        long todayPaymentCount = paymentRepository.countTodaySuccessfulPayments(userId);
        
        loggingService.logDailyLimitCheck(userId, tier.name(), todayPaymentCount, tier.getDailyLimit());
        
        if (todayPaymentCount >= tier.getDailyLimit()) {
            loggingService.logDailyLimitExceeded(userId, tier.name(), todayPaymentCount, tier.getDailyLimit());
            paymentMetrics.recordDailyLimitExceeded(userId, tier.name());
            throw new ApiException(ErrorCode.PAYMENT_DAILY_LIMIT_EXCEEDED);
        }
    }

    /**
     * Verifies that the given payment belongs to the specified user.
     *
     * @param payment the payment to check ownership of
     * @param userId the ID of the user expected to own the payment
     * @throws ApiException with ErrorCode.PAYMENT_FORBIDDEN if the payment's owner ID does not match {@code userId}
     */
    public void validatePaymentOwnership(Payment payment, Long userId) {
        if (!payment.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.PAYMENT_FORBIDDEN);
        }
    }

    /**
     * Ensures the payment's status permits cancellation.
     *
     * @param payment the payment to validate
     * @throws ApiException if the payment's status does not allow cancellation (ErrorCode.PAYMENT_INVALID_STATUS)
     */
    public void validateCancelableStatus(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PENDING && payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new ApiException(ErrorCode.PAYMENT_INVALID_STATUS);
        }
    }

    /**
     * Checks that the payment's status allows a refund.
     *
     * @param payment the payment to validate
     * @throws ApiException if the payment's status does not permit refunds (ErrorCode.PAYMENT_INVALID_STATUS)
     */
    public void validateRefundableStatus(Payment payment) {
        if (!payment.getStatus().isRefundable()) {
            throw new ApiException(ErrorCode.PAYMENT_INVALID_STATUS);
        }
    }

    /**
     * Determine and validate the refund amount for a payment.
     *
     * If `requestedAmount` is `null`, the payment's refundable amount is used. The resulting
     * refund amount must not exceed the payment's refundable amount.
     *
     * @param requestedAmount the requested refund amount, or `null` to refund the full refundable amount
     * @param payment the payment whose refundable amount will be used for validation
     * @return the validated refund amount to be processed
     * @throws ApiException if the determined refund amount is greater than the payment's refundable amount (ErrorCode.PAYMENT_REFUND_AMOUNT_EXCEEDED)
     */
    public BigDecimal validateRefundAmount(BigDecimal requestedAmount, Payment payment) {
        BigDecimal refundAmount = requestedAmount;
        if (refundAmount == null) {
            refundAmount = payment.getRefundableAmount();
        }

        if (refundAmount.compareTo(payment.getRefundableAmount()) > 0) {
            throw new ApiException(ErrorCode.PAYMENT_REFUND_AMOUNT_EXCEEDED);
        }

        return refundAmount;
    }
}
