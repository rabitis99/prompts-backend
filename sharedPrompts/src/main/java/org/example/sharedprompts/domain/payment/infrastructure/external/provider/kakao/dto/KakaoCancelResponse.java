package org.example.sharedprompts.domain.payment.infrastructure.external.provider.kakao.dto;

import java.time.LocalDateTime;

/**
 * KakaoPay 결제 취소 응답 DTO
 */
public record KakaoCancelResponse(LocalDateTime canceledAt, String metadata) {
}

