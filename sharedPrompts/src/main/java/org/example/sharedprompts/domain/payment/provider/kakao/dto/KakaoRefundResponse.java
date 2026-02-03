package org.example.sharedprompts.domain.payment.provider.kakao.dto;

import java.time.LocalDateTime;

/**
 * KakaoPay 결제 환불 응답 DTO
 */
public record KakaoRefundResponse(long refundedAmount, LocalDateTime refundedAt, String metadata) {
}

