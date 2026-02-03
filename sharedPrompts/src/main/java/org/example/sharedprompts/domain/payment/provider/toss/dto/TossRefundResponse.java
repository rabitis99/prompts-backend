package org.example.sharedprompts.domain.payment.provider.toss.dto;

import java.time.LocalDateTime;

/**
 * TossPay 결제 환불 응답
 */
public record TossRefundResponse(long refundedAmount, LocalDateTime refundedAt, String metadata) {
}
