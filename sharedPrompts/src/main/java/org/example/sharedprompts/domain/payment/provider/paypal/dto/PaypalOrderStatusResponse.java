package org.example.sharedprompts.domain.payment.provider.paypal.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * PayPal 주문 상태 조회 응답
 */
public record PaypalOrderStatusResponse(
        String status,
        BigDecimal amount,
        String currency,
        Instant approvedAt,
        String metadata
) {
    public PaypalOrderStatusResponse {
        if (status == null || status.isEmpty()) {
            throw new IllegalArgumentException("status는 필수입니다");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount는 0보다 커야 합니다");
        }
        if (currency == null || currency.isEmpty()) {
            throw new IllegalArgumentException("currency는 필수입니다");
        }
        if (approvedAt == null) {
            throw new IllegalArgumentException("approvedAt는 필수입니다");
        }
    }
}
