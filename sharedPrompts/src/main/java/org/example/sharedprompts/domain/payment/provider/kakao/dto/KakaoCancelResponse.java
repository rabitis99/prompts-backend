package org.example.sharedprompts.domain.payment.provider.kakao.dto;

import java.time.LocalDateTime;

/**
 * KakaoPay 결제 취소 응답 DTO
 */
public record KakaoCancelResponse(LocalDateTime canceledAt, String metadata) {
}

