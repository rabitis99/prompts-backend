package org.example.sharedprompts.domain.payment.validator;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
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
     * @param expectedAmount 예상 금액 (DB에 저장된 금액)
     * @param actualAmount 실제 금액 (외부 API 응답 금액)
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

}

