package org.example.sharedprompts.domain.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
}

