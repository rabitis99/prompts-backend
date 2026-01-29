package org.example.sharedprompts.domain.payment.enums;

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
     * Maximum number of payments allowed per day for this user tier.
     *
     * @return the maximum number of payments allowed per day
     */
    public int getDailyLimit() {
        return dailyPaymentLimit;
    }
}
