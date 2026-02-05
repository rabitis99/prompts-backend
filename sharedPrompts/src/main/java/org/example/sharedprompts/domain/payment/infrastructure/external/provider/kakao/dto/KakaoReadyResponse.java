package org.example.sharedprompts.domain.payment.infrastructure.external.provider.kakao.dto;

/**
 * KakaoPay 결제 준비 응답 DTO
 */
public record KakaoReadyResponse(String tid, String redirectUrl, String metadata) {
}

