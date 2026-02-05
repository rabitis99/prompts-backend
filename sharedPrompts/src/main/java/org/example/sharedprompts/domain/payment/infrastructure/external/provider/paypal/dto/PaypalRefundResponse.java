package org.example.sharedprompts.domain.payment.infrastructure.external.provider.paypal.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * PayPal 환불 응답
 */
public record PaypalRefundResponse(
        BigDecimal refundedAmount,
        String status,
        Instant refundedAt,
        String metadata
) {
}
