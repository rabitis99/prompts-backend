package org.example.sharedprompts.domain.payment.provider.kakao.dto;

import java.time.LocalDateTime;

/**
 * KakaoPay 결제 승인 응답 DTO
 */
public record KakaoApproveResponse(String status, LocalDateTime approvedAt, String metadata) {
    public KakaoApproveResponse {
        if (status == null || status.isEmpty()) {
            throw new IllegalArgumentException("status는 필수입니다");
        }
        if (approvedAt == null) {
            throw new IllegalArgumentException("approvedAt는 필수입니다");
        }
    }
}

