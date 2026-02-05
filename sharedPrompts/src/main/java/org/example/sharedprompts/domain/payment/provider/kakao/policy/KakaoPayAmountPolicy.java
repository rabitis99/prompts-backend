package org.example.sharedprompts.domain.payment.provider.kakao.policy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * KakaoPay 금액 정책
 */
@Component
public class KakaoPayAmountPolicy {

    public long toKakaoAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("KakaoPay 금액은 null일 수 없습니다");
        }
        if (amount.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("KakaoPay는 소수점 결제를 지원하지 않습니다: amount=" + amount);
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("KakaoPay 금액은 0보다 커야 합니다: amount=" + amount);
        }
        return amount.longValueExact();
    }

    public BigDecimal fromKakaoAmount(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("KakaoPay 금액은 0보다 커야 합니다: amount=" + amount);
        }
        return BigDecimal.valueOf(amount);
    }
}