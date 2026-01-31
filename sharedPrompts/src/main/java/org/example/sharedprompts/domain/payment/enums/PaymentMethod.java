package org.example.sharedprompts.domain.payment.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;

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
     * 문자열로부터 PaymentMethod를 안전하게 변환
     * 예상치 못한 provider 문자열에 대한 방어 처리
     */
    @JsonCreator
    public static PaymentMethod fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "payment_method");
        }

        try {
            // 대소문자 구분 없이 변환 시도
            return PaymentMethod.valueOf(value.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, 
                    "지원하지 않는 결제 수단입니다: " + value);
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

