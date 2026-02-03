package org.example.sharedprompts.dto.payment.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;

/**
 * 결제 승인 요청 DTO
 * 
 * <p>결제사별 필드:
 * <ul>
 *   <li>토스페이먼츠: paymentKey (필수)</li>
 *   <li>카카오페이: pgToken (필수), paymentKey는 tid (ready 시 받은 값)</li>
 * </ul>
 */
@Getter
@NoArgsConstructor
public class PaymentConfirmRequest {
    private String orderId;
    private long amount;
    private String paymentKey;  // 토스페이먼츠: paymentKey, 카카오페이: tid
    
    /**
     * 카카오페이 결제 승인 토큰 (pg_token)
     * 
     * <p>카카오페이 결제 승인 시 필수입니다.
     * - 결제 페이지에서 사용자가 결제를 승인한 후 카카오가 redirect URL에 전달
     * - 프론트엔드에서 추출하여 서버로 전달
     * - 1회성 토큰이므로 승인 API 호출 후 즉시 무효화됨
     * - DB에 저장하지 않음
     */
    private String pgToken;

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

