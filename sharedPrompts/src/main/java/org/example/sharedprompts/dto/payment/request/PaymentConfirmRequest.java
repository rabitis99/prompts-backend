package org.example.sharedprompts.dto.payment.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
    @JsonProperty("order_id")
    private String orderId;
    
    @JsonProperty("amount")
    private long amount;
    
    /**
     * 결제 세션 키
     * - 토스페이먼츠: Toss 위젯에서 받은 paymentKey (tgen_ 또는 t로 시작)
     * - 카카오페이: ready API에서 받은 tid
     */
    @JsonProperty("payment_key")
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
    @JsonProperty("pg_token")
    private String pgToken;
    
    /**
     * Toss Payments 위젯에서 사용한 orderId
     * 
     * <p>Toss Payments 결제 승인 시 선택적으로 사용됩니다.
     * - 프론트엔드에서 Toss 위젯에 전달한 orderId를 그대로 전달
     * - 예: "ORDER-123-1704067200000"
     * - 없으면 백엔드에서 payment.getId()를 사용 (기존 동작)
     * - 프론트엔드에서 주는 번호를 그대로 신뢰하여 Toss Payments API에 전달
     */
    @JsonProperty("toss_order_id")
    private String tossOrderId;

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

