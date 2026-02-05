package org.example.sharedprompts.domain.payment.infrastructure.rule;

import org.example.sharedprompts.domain.payment.domain.enums.UserTier;

public interface PaymentLimitRule {

    void validateDailyLimit(Long userId, UserTier tier, long todayPaymentCount);
}

