package org.example.sharedprompts.dto.payment.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;

/**
 * 토스페이먼츠 결제 승인 요청 DTO
 */
@Getter
@NoArgsConstructor
public class PaymentConfirmRequest {
    private String orderId;
    private long amount;
    private String paymentKey;

    public PaymentConfirmRequest(String orderId, long amount, String paymentKey) {
        this.orderId = orderId;
        this.amount = amount;
        this.paymentKey = paymentKey;
    }

    /**
     * 주문 ID를 Long으로 변환
     * @throws ApiException NumberFormatException 발생 시 INVALID_INPUT_VALUE 에러 코드와 함께 예외 발생
     */
    public Long getOrderIdAsLong() {
        try {
            return Long.parseLong(this.orderId);
        } catch (NumberFormatException e) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "orderId", 
                    "주문 ID는 숫자여야 합니다: " + this.orderId);
        }
    }
}

