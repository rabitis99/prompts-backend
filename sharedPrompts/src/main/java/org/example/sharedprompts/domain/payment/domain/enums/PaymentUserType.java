package org.example.sharedprompts.domain.payment.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 결제 사용자 타입 Enum
 */
@Getter
@RequiredArgsConstructor
public enum PaymentUserType {
    PERSONAL("개인"),
    BUSINESS("회사");

    private final String description;
}

