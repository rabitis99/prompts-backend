package org.example.sharedprompts.domain.payment.provider.kakao.policy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * KakaoPay 금액 정책
 * 
 * <p>단일 책임: KakaoPay 금액 검증 및 변환만 담당
 */
@Component
public class KakaoPayAmountPolicy {

    /**
     * BigDecimal을 KakaoPay 금액(long, 원 단위)으로 변환
     * 
     * @param amount 결제 금액 (필수)
     * @return KakaoPay 금액 (원 단위)
     * @throws IllegalArgumentException amount가 null이거나 소수점이 있을 때
     */
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

    /**
     * KakaoPay 금액(long, 원 단위)을 BigDecimal로 변환
     * 
     * @param amount KakaoPay 금액 (원 단위, 필수)
     * @return BigDecimal 금액
     * @throws IllegalArgumentException amount가 0 이하일 때
     */
    public BigDecimal fromKakaoAmount(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("KakaoPay 금액은 0보다 커야 합니다: amount=" + amount);
        }
        return BigDecimal.valueOf(amount);
    }
}