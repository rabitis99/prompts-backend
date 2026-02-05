package org.example.sharedprompts.domain.payment.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 티어 Enum
 */
@Getter
@RequiredArgsConstructor
public enum UserTier {
    FREE("무료", 15),
    PRO("프로", 100),
    PREMIUM("프리미엄", 300);

    private final String description;
    private final int dailyPaymentLimit; // 일일 최대 결제 횟수

    /**
     * 티어에 따른 일일 결제 제한 횟수를 반환합니다.
     */
    public int getDailyLimit() {
        return dailyPaymentLimit;
    }
}

