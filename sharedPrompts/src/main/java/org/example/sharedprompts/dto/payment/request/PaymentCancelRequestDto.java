package org.example.sharedprompts.dto.payment.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;

/**
 * 결제 취소 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCancelRequestDto {

    @NotBlank(message = "결제 ID를 입력해주세요.")
    @JsonProperty("payment_id")
    private String paymentId;

    @JsonProperty("reason")
    private String reason; // 취소 사유

    /**
     * 결제 ID를 Long으로 변환
     * @throws ApiException NumberFormatException 발생 시 INVALID_INPUT_VALUE 에러 코드와 함께 예외 발생
     */
    public Long getPaymentIdAsLong() {
        try {
            return Long.parseLong(this.paymentId);
        } catch (NumberFormatException e) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "paymentId", 
                    "결제 ID는 숫자여야 합니다: " + this.paymentId);
        }
    }

    /**
     * 취소 사유 반환 (null인 경우 기본값)
     */
    public String getReasonOrDefault() {
        return this.reason != null ? this.reason : "사용자 요청";
    }
}

