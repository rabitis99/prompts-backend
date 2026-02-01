package org.example.sharedprompts.domain.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 포인트 타입 Enum
 */
@Getter
@RequiredArgsConstructor
public enum PointType {
    PAYMENT("결제 적립"),
    CASHBACK("캐시백 전환"),
    PROMOTION("프로모션"),
    EVENT("이벤트"),
    REFERRAL("추천인 적립"),
    ADMIN("관리자 지급"),
    USE("포인트 사용");

    private final String description;
}

