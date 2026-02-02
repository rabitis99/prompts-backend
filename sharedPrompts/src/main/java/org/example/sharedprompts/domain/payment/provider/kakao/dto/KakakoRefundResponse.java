package org.example.sharedprompts.domain.payment.provider.kakao.dto;

import java.time.LocalDateTime;

/**
 * KakaoPay 결제 환불 응답 DTO
 */
public record KakakoRefundResponse(long refundedAmount, LocalDateTime refundedAt, String metadata) {
    public KakakoRefundResponse {
        if (refundedAmount <= 0) {
            throw new IllegalArgumentException("refundedAmount는 0보다 커야 합니다: " + refundedAmount);
        }
        if (refundedAt == null) {
            throw new IllegalArgumentException("refundedAt는 필수입니다");
        }
    }
}

