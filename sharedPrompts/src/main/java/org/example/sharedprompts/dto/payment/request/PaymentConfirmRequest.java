package org.example.sharedprompts.dto.payment.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;

/**
 * 결제 승인 요청 DTO
 * 
 * <p>결제사별 필드:
 * <ul>
 *   <li>토스페이먼츠: paymentKey (필수, tgen_ 또는 t로 시작하는 실제 결제 세션 키)</li>
 *   <li>카카오페이: pgToken (필수), paymentKey는 tid (ready 시 받은 값)</li>
 * </ul>
 */
@Getter
@NoArgsConstructor
public class PaymentConfirmRequest {
    private String orderId;
    private long amount;
    /**
     * 결제 세션 키
     * - 토스페이먼츠: Toss 위젯에서 받은 paymentKey (tgen_ 또는 t로 시작)
     * - 카카오페이: ready API에서 받은 tid
     */
    private String paymentKey;
    
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
    
    /**
     * paymentKey 형식 검증 (결제사별)
     * 
     * <p>Toss의 경우:
     * - paymentKey는 tgen_ 또는 t로 시작해야 함
     * - PaymentMethod.TOSS.name() 값("Toss")과 혼동 방지
     * 
     * @param paymentMethod 결제 수단
     * @throws ApiException 형식이 올바르지 않은 경우
     */
    public void validatePaymentKeyFormat(PaymentMethod paymentMethod) {
        if (this.paymentKey == null || this.paymentKey.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "paymentKey",
                    "paymentKey는 필수입니다");
        }
        
        if (paymentMethod == PaymentMethod.TOSS) {
            // Toss paymentKey 형식 검증: tgen_ 또는 t로 시작
            if (!this.paymentKey.matches("^t(gen_|\\w+).*")) {
                throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "paymentKey",
                        "Toss paymentKey 형식이 올바르지 않습니다: " + this.paymentKey);
            }
            // PaymentMethod 값과 혼동 방지
            if ("Toss".equals(this.paymentKey) || "TOSS".equals(this.paymentKey)) {
                throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "paymentKey",
                        "paymentKey는 PaymentMethod가 아닌 실제 결제 세션 키여야 합니다");
            }
        }
    }
}

