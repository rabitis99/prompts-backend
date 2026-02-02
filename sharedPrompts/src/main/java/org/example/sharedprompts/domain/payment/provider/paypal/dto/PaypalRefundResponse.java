package org.example.sharedprompts.domain.payment.provider.paypal.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PayPal 환불 응답
 */
public record PaypalRefundResponse(
        BigDecimal refundedAmount,
        String status,
        LocalDateTime refundedAt,
        String metadata
) {
    public PaypalRefundResponse {
        if (refundedAmount == null || refundedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("refundedAmount는 0보다 커야 합니다");
        }
        if (refundedAt == null) {
            throw new IllegalArgumentException("refundedAt는 필수입니다");
        }
    }
}
