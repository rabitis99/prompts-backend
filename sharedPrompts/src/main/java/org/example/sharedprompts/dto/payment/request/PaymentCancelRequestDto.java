package org.example.sharedprompts.dto.payment.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 취소 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCancelRequestDto {

    @NotBlank(message = "결제 ID를 입력해주세요.")
    private String paymentId;

    private String reason; // 취소 사유

    /**
     * 결제 ID를 Long으로 변환
     */
    public Long getPaymentIdAsLong() {
        return Long.parseLong(this.paymentId);
    }

    /**
     * 취소 사유 반환 (null인 경우 기본값)
     */
    public String getReasonOrDefault() {
        return this.reason != null ? this.reason : "사용자 요청";
    }
}

