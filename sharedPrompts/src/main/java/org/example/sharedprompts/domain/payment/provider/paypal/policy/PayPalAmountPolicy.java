package org.example.sharedprompts.domain.payment.provider.paypal.policy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * PayPal 금액 정책
 * 
 * <p>단일 책임: PayPal 금액 검증만 담당
 */
@Component
public class PayPalAmountPolicy {

    /**
     * PayPal 금액 검증
     * 
     * @param amount 결제 금액 (필수)
     * @return 검증된 금액
     * @throws IllegalArgumentException amount가 null이거나 0 이하일 때
     */
    public BigDecimal validate(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("PayPal 금액은 null일 수 없습니다");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("PayPal 금액은 0보다 커야 합니다: amount=" + amount);
        }
        return amount;
    }
}

