package org.example.sharedprompts.domain.payment.provider.toss.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TossPay 결제 승인 응답
 */
public record TossConfirmResponse(
        String paymentKey,
        String status,
        BigDecimal totalAmount,
        String currency,
        String orderId,
        LocalDateTime approvedAt,
        String metadata
) {
    public TossConfirmResponse {
        if (paymentKey == null || paymentKey.isEmpty()) {
            throw new IllegalArgumentException("paymentKey는 필수입니다");
        }
        if (status == null || status.isEmpty()) {
            throw new IllegalArgumentException("status는 필수입니다");
        }
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("totalAmount는 0보다 커야 합니다");
        }
        if (currency == null || currency.isEmpty()) {
            throw new IllegalArgumentException("currency는 필수입니다");
        }
        if (orderId == null || orderId.isEmpty()) {
            throw new IllegalArgumentException("orderId는 필수입니다");
        }
        if (approvedAt == null) {
            throw new IllegalArgumentException("approvedAt는 필수입니다");
        }
    }
}
