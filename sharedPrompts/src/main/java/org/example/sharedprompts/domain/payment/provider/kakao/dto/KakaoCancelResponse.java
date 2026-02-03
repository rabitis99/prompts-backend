package org.example.sharedprompts.domain.payment.provider.kakao.dto;

import java.time.LocalDateTime;

/**
 * KakaoPay 결제 취소 응답 DTO
 */
public record KakaoCancelResponse(LocalDateTime canceledAt, String metadata) {
    public KakaoCancelResponse {
        if (canceledAt == null) {
            throw new IllegalArgumentException("canceledAt는 필수입니다");
        }
    }
}

