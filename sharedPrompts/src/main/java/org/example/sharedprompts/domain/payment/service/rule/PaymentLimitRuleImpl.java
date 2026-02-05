package org.example.sharedprompts.domain.payment.service.rule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.enums.UserTier;
import org.example.sharedprompts.domain.payment.logging.PaymentLoggingService;
import org.example.sharedprompts.domain.payment.metrics.PaymentMetrics;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 결제 제한 규칙 구현체
 *
 * <p><strong>현재 정책:</strong>
 * 티어별 일일 결제 제한을 검증합니다.
 *
 * @see PaymentLimitRule
 */
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

