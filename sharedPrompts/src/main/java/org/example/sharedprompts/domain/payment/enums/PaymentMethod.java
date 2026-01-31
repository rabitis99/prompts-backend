package org.example.sharedprompts.domain.payment.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.exception.PaymentMethodException;

/**
 * 결제 수단 Enum
 */
@Getter
@RequiredArgsConstructor
public enum PaymentMethod {
    KAKAO_PAY("카카오페이"),
    TOSS("토스"),
    PAYPAL("페이팔");

    private final String description;

    /**
     * 문자열로부터 PaymentMethod를 변환
     * 검증은 서비스 레이어에서 수행하며, 여기서는 매핑만 담당합니다.
     */
    @JsonCreator
    public static PaymentMethod fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new PaymentMethodException("PaymentMethod cannot be null or blank");
        }

        try {
            // 대소문자 구분 없이 변환 시도
            return PaymentMethod.valueOf(value.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            throw new PaymentMethodException("Unsupported payment method: " + value, e);
        }
    }

    /**
     * JSON 직렬화 시 사용
     */
    @JsonValue
    public String toValue() {
        return this.name();
    }
}

