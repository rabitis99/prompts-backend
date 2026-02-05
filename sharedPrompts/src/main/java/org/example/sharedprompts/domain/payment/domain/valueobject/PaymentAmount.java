package org.example.sharedprompts.domain.payment.domain.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 결제 금액 값 객체
 * 
 * <p>금액과 통화를 함께 관리하는 불변 객체입니다.
 * 금액 계산 로직을 캡슐화하여 도메인 규칙을 보장합니다.
 */
@Getter
@EqualsAndHashCode
public class PaymentAmount {
    
    private final BigDecimal amount;
    private final Currency currency;
    
    private PaymentAmount(BigDecimal amount, Currency currency) {
        if (amount == null) {
            throw new IllegalArgumentException("금액은 필수입니다.");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("금액은 0 이상이어야 합니다: " + amount);
        }
        if (currency == null) {
            throw new IllegalArgumentException("통화는 필수입니다.");
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency;
    }
    
    /**
     * PaymentAmount 객체 생성
     */
    public static PaymentAmount of(BigDecimal amount, Currency currency) {
        return new PaymentAmount(amount, currency);
    }
    
    /**
     * PaymentAmount 객체 생성 (문자열 통화 코드)
     */
    public static PaymentAmount of(BigDecimal amount, String currencyCode) {
        return new PaymentAmount(amount, Currency.of(currencyCode));
    }
    
    /**
     * 한국 원화로 PaymentAmount 생성
     */
    public static PaymentAmount krw(BigDecimal amount) {
        return new PaymentAmount(amount, Currency.KRW());
    }
    
    /**
     * 미국 달러로 PaymentAmount 생성
     */
    public static PaymentAmount usd(BigDecimal amount) {
        return new PaymentAmount(amount, Currency.USD());
    }
    
    /**
     * 금액 더하기 (같은 통화만 가능)
     */
    public PaymentAmount add(PaymentAmount other) {
        validateSameCurrency(other);
        return new PaymentAmount(this.amount.add(other.amount), this.currency);
    }
    
    /**
     * 금액 빼기 (같은 통화만 가능)
     */
    public PaymentAmount subtract(PaymentAmount other) {
        validateSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("결과 금액이 음수가 될 수 없습니다.");
        }
        return new PaymentAmount(result, this.currency);
    }
    
    /**
     * 금액 곱하기 (비율 적용)
     */
    public PaymentAmount multiply(BigDecimal ratio) {
        if (ratio == null || ratio.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("비율은 0 이상이어야 합니다: " + ratio);
        }
        return new PaymentAmount(this.amount.multiply(ratio), this.currency);
    }
    
    /**
     * 금액 나누기 (비율 계산)
     */
    public PaymentAmount divide(BigDecimal divisor) {
        if (divisor == null || divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("나눌 값은 0이 될 수 없습니다: " + divisor);
        }
        return new PaymentAmount(this.amount.divide(divisor, 2, RoundingMode.HALF_UP), this.currency);
    }
    
    /**
     * 금액 비교 (같은 통화만 가능)
     */
    public int compareTo(PaymentAmount other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount);
    }
    
    /**
     * 0보다 큰지 확인
     */
    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * 0인지 확인
     */
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }
    
    /**
     * 다른 금액보다 큰지 확인
     */
    public boolean isGreaterThan(PaymentAmount other) {
        return compareTo(other) > 0;
    }
    
    /**
     * 다른 금액보다 작은지 확인
     */
    public boolean isLessThan(PaymentAmount other) {
        return compareTo(other) < 0;
    }
    
    /**
     * 다른 금액과 같은지 확인
     */
    public boolean isEqualTo(PaymentAmount other) {
        return compareTo(other) == 0;
    }
    
    /**
     * 통화가 같은지 확인
     */
    public boolean hasSameCurrency(PaymentAmount other) {
        return this.currency.equals(other.currency);
    }
    
    /**
     * BigDecimal 금액 반환 (JPA 매핑용)
     */
    public BigDecimal toBigDecimal() {
        return this.amount;
    }
    
    /**
     * 통화 코드 문자열 반환
     */
    public String getCurrencyCode() {
        return this.currency.getCode();
    }
    
    /**
     * 같은 통화인지 검증
     */
    private void validateSameCurrency(PaymentAmount other) {
        if (other == null) {
            throw new IllegalArgumentException("비교할 금액이 null입니다.");
        }
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                String.format("통화가 다릅니다. this=%s, other=%s", this.currency, other.currency)
            );
        }
    }
    
    @Override
    public String toString() {
        return String.format("%s %s", amount, currency);
    }
}

