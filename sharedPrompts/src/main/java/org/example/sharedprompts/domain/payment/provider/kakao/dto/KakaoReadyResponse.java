package org.example.sharedprompts.domain.payment.provider.kakao.dto;

/**
 * KakaoPay 결제 준비 응답 DTO
 */
public record KakaoReadyResponse(String tid, String redirectUrl, String metadata) {
    public KakaoReadyResponse {
        if (tid == null || tid.isEmpty()) {
            throw new IllegalArgumentException("tid는 필수입니다");
        }
        if (redirectUrl == null || redirectUrl.isEmpty()) {
            throw new IllegalArgumentException("redirectUrl은 필수입니다");
        }
    }
}

