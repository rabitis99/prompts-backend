package org.example.sharedprompts.domain.payment.infrastructure.rule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.enums.UserTier;
import org.example.sharedprompts.domain.payment.infrastructure.monitoring.PaymentLoggingService;
import org.example.sharedprompts.domain.payment.infrastructure.monitoring.PaymentMetrics;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentLimitRuleImpl implements PaymentLimitRule {

    private final PaymentLoggingService loggingService;
    private final PaymentMetrics paymentMetrics;

    @Override
    public void validateDailyLimit(Long userId, UserTier tier, long todayPaymentCount) {
        int dailyLimit = tier.getDailyLimit();
        
        loggingService.logDailyLimitCheck(userId, tier.name(), todayPaymentCount, dailyLimit);
        
        if (todayPaymentCount >= dailyLimit) {
            loggingService.logDailyLimitExceeded(userId, tier.name(), todayPaymentCount, dailyLimit);
            paymentMetrics.recordDailyLimitExceeded(userId, tier.name());
            throw new ApiException(ErrorCode.PAYMENT_DAILY_LIMIT_EXCEEDED);
        }
    }
}

