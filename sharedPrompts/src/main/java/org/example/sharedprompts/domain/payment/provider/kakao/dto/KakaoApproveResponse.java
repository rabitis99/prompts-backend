package org.example.sharedprompts.domain.payment.provider.kakao.dto;

import java.time.LocalDateTime;

/**
 * KakaoPay 결제 승인 응답 DTO
 */
public record KakaoApproveResponse(String status, LocalDateTime approvedAt, String metadata) {
}

