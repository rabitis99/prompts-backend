package org.example.sharedprompts.domain.payment.provider.kakao.dto;

/**
 * KakaoPay 결제 상태 조회 응답 DTO
 */
public record KakaoStatusResponse(String status, String orderId, long amount, long taxFreeAmount, String metadata) {
}

