package org.example.sharedprompts.domain.payment.domain.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 환율 값 객체
 * 
 * <p>환율 정보를 표현하는 불변 객체입니다.
 * 원본 통화에서 대상 통화로의 환율을 관리합니다.
 */
@Getter
@EqualsAndHashCode
public class ExchangeRate {
    
    private final Currency fromCurrency;
    private final Currency toCurrency;
    private final BigDecimal rate;
    
    private ExchangeRate(Currency fromCurrency, Currency toCurrency, BigDecimal rate) {
        if (fromCurrency == null) {
            throw new IllegalArgumentException("원본 통화는 필수입니다.");
        }
        if (toCurrency == null) {
            throw new IllegalArgumentException("대상 통화는 필수입니다.");
        }
        if (fromCurrency.equals(toCurrency)) {
            throw new IllegalArgumentException("원본 통화와 대상 통화가 같을 수 없습니다.");
        }
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("환율은 0보다 커야 합니다: " + rate);
        }
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.rate = rate.setScale(6, RoundingMode.HALF_UP);
    }
    
    /**
     * ExchangeRate 객체 생성
     */
    public static ExchangeRate of(Currency fromCurrency, Currency toCurrency, BigDecimal rate) {
        return new ExchangeRate(fromCurrency, toCurrency, rate);
    }
    
    /**
     * ExchangeRate 객체 생성 (문자열 통화 코드)
     */
    public static ExchangeRate of(String fromCurrencyCode, String toCurrencyCode, BigDecimal rate) {
        return new ExchangeRate(
            Currency.of(fromCurrencyCode),
            Currency.of(toCurrencyCode),
            rate
        );
    }
    
    /**
     * 환율을 사용하여 금액 변환
     */
    public PaymentAmount convert(PaymentAmount amount) {
        if (!amount.getCurrency().equals(fromCurrency)) {
            throw new IllegalArgumentException(
                String.format("원본 통화가 일치하지 않습니다. expected=%s, actual=%s", 
                    fromCurrency, amount.getCurrency())
            );
        }
        BigDecimal convertedAmount = amount.toBigDecimal().multiply(rate);
        return PaymentAmount.of(convertedAmount, toCurrency);
    }
    
    /**
     * 역환율 계산 (toCurrency -> fromCurrency)
     */
    public ExchangeRate reverse() {
        BigDecimal reverseRate = BigDecimal.ONE.divide(rate, 6, RoundingMode.HALF_UP);
        return new ExchangeRate(toCurrency, fromCurrency, reverseRate);
    }
    
    @Override
    public String toString() {
        return String.format("%s -> %s: %s", fromCurrency, toCurrency, rate);
    }
}

