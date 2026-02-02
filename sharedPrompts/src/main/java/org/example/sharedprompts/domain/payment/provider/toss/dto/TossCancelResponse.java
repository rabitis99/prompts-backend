package org.example.sharedprompts.domain.payment.provider.toss.dto;

import java.time.LocalDateTime;

/**
 * TossPay 결제 취소 응답
 */
public record TossCancelResponse(LocalDateTime canceledAt, String metadata) {
    public TossCancelResponse {
        if (canceledAt == null) {
            throw new IllegalArgumentException("canceledAt는 필수입니다");
        }
    }
}
