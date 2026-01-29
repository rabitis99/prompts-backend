package org.example.sharedprompts.domain.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 결제 상태 Enum
 */
@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
    PENDING("대기중"),
    SUCCESS("성공"),
    FAILED("실패"),
    CANCELED("취소됨"),
    REFUNDED("환불됨"),
    PARTIALLY_REFUNDED("부분 환불됨");

    private final String description;

    /**
     * 결제가 완료된 상태인지 확인합니다.
     */
    public boolean isCompleted() {
        return this == SUCCESS;
    }

    /**
     * 결제가 취소/환불 가능한 상태인지 확인합니다.
     */
    public boolean isRefundable() {
        return this == SUCCESS || this == PARTIALLY_REFUNDED;
    }
}

