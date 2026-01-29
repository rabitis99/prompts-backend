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
     * Convert the stored payment ID string to a Long.
     *
     * @return the payment ID as a Long
     */
    public Long getPaymentIdAsLong() {
        return Long.parseLong(this.paymentId);
    }

    /**
     * Provide the cancellation reason or a default when none is provided.
     *
     * @return the cancellation reason if set; otherwise "사용자 요청"
     */
    public String getReasonOrDefault() {
        return this.reason != null ? this.reason : "사용자 요청";
    }
}
