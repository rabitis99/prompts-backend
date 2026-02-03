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
}
