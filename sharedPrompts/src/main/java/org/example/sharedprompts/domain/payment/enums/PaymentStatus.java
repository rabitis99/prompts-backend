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
     * Determines whether the payment status represents a completed payment.
     *
     * @return true if the status is SUCCESS, false otherwise.
     */
    public boolean isCompleted() {
        return this == SUCCESS;
    }

    /**
     * Determines whether the payment status permits a refund or cancellation.
     *
     * @return `true` if the status is `SUCCESS` or `PARTIALLY_REFUNDED`, `false` otherwise.
     */
    public boolean isRefundable() {
        return this == SUCCESS || this == PARTIALLY_REFUNDED;
    }
}
