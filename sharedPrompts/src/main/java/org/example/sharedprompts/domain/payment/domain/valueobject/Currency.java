package org.example.sharedprompts.domain.payment.domain.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Set;

/**
 * 통화 코드 값 객체
 * 
 * <p>ISO 4217 표준 통화 코드를 표현합니다.
 * 불변 객체로 설계되어 값의 일관성을 보장합니다.
 */
@Getter
@EqualsAndHashCode
public class Currency {
    
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of(
        "KRW", "USD", "EUR", "JPY", "CNY", "GBP"
    );
    
    private final String code;
    
    private Currency(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("통화 코드는 필수입니다.");
        }
        if (code.length() != 3) {
            throw new IllegalArgumentException("통화 코드는 3자리여야 합니다: " + code);
        }
        String upperCode = code.toUpperCase();
        if (!SUPPORTED_CURRENCIES.contains(upperCode)) {
            throw new IllegalArgumentException("지원하지 않는 통화 코드입니다: " + code);
        }
        this.code = upperCode;
    }
    
    /**
     * 통화 코드로 Currency 객체 생성
     */
    public static Currency of(String code) {
        return new Currency(code);
    }
    
    /**
     * 한국 원화 Currency 객체
     */
    public static Currency KRW() {
        return new Currency("KRW");
    }
    
    /**
     * 미국 달러 Currency 객체
     */
    public static Currency USD() {
        return new Currency("USD");
    }
    
    /**
     * 유로 Currency 객체
     */
    public static Currency EUR() {
        return new Currency("EUR");
    }
    
    /**
     * 통화 코드 문자열 반환
     */
    @Override
    public String toString() {
        return code;
    }
}

