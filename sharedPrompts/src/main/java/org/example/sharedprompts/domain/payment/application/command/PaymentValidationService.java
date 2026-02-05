package org.example.sharedprompts.domain.payment.application.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.domain.enums.UserTier;
import org.example.sharedprompts.domain.payment.domain.service.PaymentDomainService;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.domain.payment.infrastructure.rule.PaymentLimitRule;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentValidationService {

    private final PaymentJpaAdapter paymentJpaAdapter;
    private final PaymentLimitRule paymentLimitRule;
    private final PaymentDomainService domainService;

    public void validateDailyLimit(Long userId, UserTier tier) {
        long todayPaymentCount = paymentJpaAdapter.countTodaySuccessfulPayments(userId, PaymentStatus.SUCCESS);
        paymentLimitRule.validateDailyLimit(userId, tier, todayPaymentCount);
    }

    public void validatePaymentOwnership(Payment payment, Long userId) {
        if (!payment.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.PAYMENT_FORBIDDEN);
        }
    }

    public void validateCancelableStatus(Payment payment) {
        domainService.validateCanCancel(payment);
    }

    public void validateRefundableStatus(Payment payment) {
        domainService.validateCanRefund(payment);
    }

    public BigDecimal validateRefundAmount(BigDecimal requestedAmount, Payment payment) {
        BigDecimal refundAmount = requestedAmount;
        if (refundAmount == null) {
            refundAmount = payment.getRefundableAmount();
        }
        domainService.validateRefundAmount(payment, refundAmount);
        return refundAmount;
    }

    public boolean canTransitionTo(Payment payment, PaymentStatus targetStatus) {
        return domainService.canTransitionTo(payment, targetStatus);
    }

    public boolean validateCanCancelPayment(Payment payment) {
        return domainService.canCancel(payment);
    }

    public boolean validateCanRefundPayment(Payment payment) {
        return domainService.canRefund(payment);
    }

    public boolean validateCanRefundPaymentAmount(Payment payment, BigDecimal refundAmount) {
        return domainService.canRefundAmount(payment, refundAmount);
    }

    public boolean isPaymentExpired(Payment payment, long expiryMinutes) {
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return false;
        }
        LocalDateTime expiryTime = payment.getCreatedAt().plusMinutes(expiryMinutes);
        return LocalDateTime.now().isAfter(expiryTime);
    }

    public boolean hasUnrecoveredPoints(Payment payment) {
        return payment.getStatus() == PaymentStatus.PENDING
                && payment.getUsedPointAmount() != null
                && payment.getUsedPointAmount().compareTo(BigDecimal.ZERO) > 0;
    }
}

