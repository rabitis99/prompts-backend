package org.example.sharedprompts.domain.payment.provider.toss.dto;

import java.time.LocalDateTime;

/**
 * TossPay 결제 환불 응답
 */
public record TossRefundResponse(long refundedAmount, LocalDateTime refundedAt, String metadata) {
    public TossRefundResponse {
        if (refundedAmount <= 0) {
            throw new IllegalArgumentException("refundedAmount는 0보다 커야 합니다: " + refundedAmount);
        }
        if (refundedAt == null) {
            throw new IllegalArgumentException("refundedAt는 필수입니다");
        }
    }
}
