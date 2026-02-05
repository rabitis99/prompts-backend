package org.example.sharedprompts.domain.payment.provider.toss.policy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * TossPay 금액 정책
 * 
 * <p>단일 책임: TossPay 금액 검증 및 변환만 담당
 */
@Component
public class TossPayAmountPolicy {

    /**
     * BigDecimal을 TossPay 금액(long, 원 단위)으로 변환
     */
    public long toTossAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("TossPay 금액은 null일 수 없습니다");
        }
        if (amount.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("TossPay는 소수점 결제를 지원하지 않습니다: amount=" + amount);
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("TossPay 금액은 0보다 커야 합니다: amount=" + amount);
        }
        return amount.longValueExact();
    }

    /**
     * TossPay 금액(long, 원 단위)을 BigDecimal로 변환
     */
    public BigDecimal fromTossAmount(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("TossPay 금액은 0보다 커야 합니다: amount=" + amount);
        }
        return BigDecimal.valueOf(amount);
    }
}

