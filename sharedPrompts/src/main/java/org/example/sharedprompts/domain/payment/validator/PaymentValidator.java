package org.example.sharedprompts.domain.payment.validator;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.model.PaymentResult;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 결제 검증 전용 컴포넌트
 * 
 * <p>단일 책임: 금액/주문번호/통화 검증만 담당
 * - 외부 API 호출과 무관
 * - 상태 변경과 무관
 */
@Slf4j
@Component
public class PaymentValidator {
    
    /**
     * 결제 금액 검증
     * 
     * @param expectedAmount 예상 금액 (결제 시 전송한 금액)
     * @param actualAmount 실제 금액 (외부 API로부터 응답받은 금액)
     * @param orderId 주문 ID
     */
    public void validateAmount(BigDecimal expectedAmount, BigDecimal actualAmount, String orderId) {
        if (expectedAmount == null || actualAmount == null) {
            log.error("결제 금액 검증 실패: 금액이 null입니다. orderId={}, expected={}, actual={}", 
                    orderId, expectedAmount, actualAmount);
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "결제 금액이 유효하지 않습니다.");
        }
        
        if (expectedAmount.compareTo(actualAmount) != 0) {
            log.error("결제 금액 불일치: orderId={}, expected={}, actual={}", 
                    orderId, expectedAmount, actualAmount);
            throw new ApiException(ErrorCode.PAYMENT_AMOUNT_MISMATCH, 
                    String.format("결제 금액이 일치하지 않습니다. 예상: %s, 실제: %s", expectedAmount, actualAmount));
        }
    }
    
    /**
     * 주문 ID 검증
     * 
     * @param expectedOrderId 예상 주문 ID (DB에 저장된 주문 ID)
     * @param actualOrderId 실제 주문 ID (외부 API 응답 주문 ID)
     */
    public void validateOrderId(String expectedOrderId, String actualOrderId) {
        if (expectedOrderId == null || actualOrderId == null) {
            log.error("주문 ID 검증 실패: 주문 ID가 null입니다. expected={}, actual={}", 
                    expectedOrderId, actualOrderId);
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "주문 ID가 유효하지 않습니다.");
        }
        
        if (!expectedOrderId.equals(actualOrderId)) {
            log.error("주문 ID 불일치: expected={}, actual={}", expectedOrderId, actualOrderId);
            throw new ApiException(ErrorCode.PAYMENT_ORDER_ID_MISMATCH, 
                    String.format("주문 ID가 일치하지 않습니다. 예상: %s, 실제: %s", expectedOrderId, actualOrderId));
        }
    }
    
    /**
     * 통화 코드 검증
     * 
     * @param expectedCurrency 예상 통화 코드
     * @param actualCurrency 실제 통화 코드
     */
    public void validateCurrency(String expectedCurrency, String actualCurrency) {
        if (expectedCurrency == null || actualCurrency == null) {
            log.error("통화 코드 검증 실패: 통화 코드가 null입니다. expected={}, actual={}", 
                    expectedCurrency, actualCurrency);
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "통화 코드가 유효하지 않습니다.");
        }
        
        if (!expectedCurrency.equalsIgnoreCase(actualCurrency)) {
            log.error("통화 코드 불일치: expected={}, actual={}", expectedCurrency, actualCurrency);
            throw new ApiException(ErrorCode.PAYMENT_CURRENCY_MISMATCH, 
                    String.format("통화 코드가 일치하지 않습니다. 예상: %s, 실제: %s", expectedCurrency, actualCurrency));
        }
    }
    
    /**
     * PaymentResult와 Payment 엔티티의 일관성 검증
     *
     * @param payment Payment 엔티티
     * @param result PaymentResult (외부 API 응답)
     * @param actualAmount 실제 결제 금액 (포인트 사용 후 금액)
     */
    public void validatePaymentResult(Payment payment, PaymentResult result, BigDecimal actualAmount) {
        validateOrderId(String.valueOf(payment.getId()), result.getOrderId());
        // 실제 결제 금액으로 검증 (포인트 사용 시 payment.getAmount()와 다를 수 있음)
        validateAmount(actualAmount, result.getAmount(), result.getOrderId());
        validateCurrency(payment.getCurrency(), result.getCurrency());
    }

    // ============ 요청 검증 메서드 ============

    /**
     * paymentKey 필수값 및 형식 검증 (결제사별)
     *
     * <p>Toss의 경우:
     * - paymentKey는 tgen_ 또는 t로 시작해야 함
     * - PaymentMethod.TOSS.name() 값("Toss")과 혼동 방지
     *
     * @param paymentKey 결제 세션 키
     * @param paymentMethod 결제 수단
     * @throws ApiException 형식이 올바르지 않은 경우
     */
    public void validatePaymentKey(String paymentKey, PaymentMethod paymentMethod) {
        if (paymentKey == null || paymentKey.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "paymentKey",
                    "paymentKey는 필수입니다");
        }

        if (paymentMethod == PaymentMethod.TOSS) {
            // Toss paymentKey 형식 검증: tgen_ 또는 t로 시작
            if (!paymentKey.matches("^t(gen_|\\w+).*")) {
                throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "paymentKey",
                        "Toss paymentKey 형식이 올바르지 않습니다: " + paymentKey);
            }
            // PaymentMethod 값과 혼동 방지
            if ("Toss".equals(paymentKey) || "TOSS".equals(paymentKey)) {
                throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "paymentKey",
                        "paymentKey는 PaymentMethod가 아닌 실제 결제 세션 키여야 합니다");
            }
        }
    }

    /**
     * 카카오페이 pgToken 검증
     *
     * @param pgToken 카카오페이 결제 승인 토큰
     * @throws ApiException pgToken이 없거나 비어있는 경우
     */
    public void validateKakaoPayPgToken(String pgToken) {
        if (pgToken == null || pgToken.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "pgToken",
                    "카카오페이 결제 승인을 위해서는 pgToken이 필수입니다");
        }
    }

}

