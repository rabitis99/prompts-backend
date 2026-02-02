package org.example.sharedprompts.domain.payment.provider.kakao.dto;

/**
 * KakaoPay 결제 상태 조회 응답 DTO
 */
public record KakakoStatusResponse(String status, String orderId, long amount, String metadata) {
    public KakakoStatusResponse {
        if (status == null || status.isEmpty()) {
            throw new IllegalArgumentException("status는 필수입니다");
        }
        if (orderId == null || orderId.isEmpty()) {
            throw new IllegalArgumentException("orderId는 필수입니다");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("amount는 0보다 커야 합니다: " + amount);
        }
    }
}

