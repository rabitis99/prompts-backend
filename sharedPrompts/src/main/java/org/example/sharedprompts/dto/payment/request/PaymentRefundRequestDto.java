package org.example.sharedprompts.dto.payment.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;

import java.math.BigDecimal;

/**
 * 결제 환불 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRefundRequestDto {

    @NotBlank(message = "결제 ID를 입력해주세요.")
    private String paymentId;

    @DecimalMin(value = "0.01", message = "환불 금액은 0.01 이상이어야 합니다.")
    private BigDecimal amount; // null이면 전체 환불, 값이 있으면 부분 환불

    private String reason; // 환불 사유

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
     * 환불 금액 반환 (null인 경우 null 반환 - 전체 환불)
     */
    public BigDecimal getAmountOrNull() {
        return this.amount;
    }

    /**
     * 환불 사유 반환 (null인 경우 기본값)
     */
    public String getReasonOrDefault() {
        return this.reason != null ? this.reason : "사용자 요청";
    }
}

