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
    USE("포인트 사용"),
    REFUND("환불 복구"),
    CANCEL("취소 복구"),
    PAYMENT_FAILED("결제 실패 복구");

    private final String description;
}

