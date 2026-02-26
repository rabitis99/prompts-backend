package org.example.sharedprompts.domain.payment.application.command;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.UserTier;
import org.example.sharedprompts.domain.payment.domain.service.PaymentDomainService;
import org.example.sharedprompts.domain.payment.infrastructure.rule.PaymentLimitRule;
import org.example.sharedprompts.domain.payment.service.user.tier.UserTierService;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentValidationService {

    private final PaymentLimitRule paymentLimitRule;
    private final PaymentDomainService domainService;
    private final UserTierService userTierService;

    public void validateDailyLimit(Long userId, UserTier tier) {
        long todayPaymentCount = userTierService.getTodayPaymentCount(userId);
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
        BigDecimal refundAmount = requestedAmount != null
                ? requestedAmount
                : payment.getRefundableAmount();

        domainService.validateRefundAmount(payment, refundAmount);
        return refundAmount;
    }
}

