package org.example.sharedprompts.domain.payment.infrastructure.external.provider.toss.exception;

/**
 * TossPayments 중복 주문번호 오류 예외
 * 
 * <p>S021 오류: 이미 사용된 주문번호로 결제를 시도할 때 발생
 * <p>이 오류는 주문번호가 이미 사용되었음을 의미하며, 
 * 결제가 이미 확인되었을 가능성이 있습니다.
 */
public class DuplicateOrderIdException extends RuntimeException {
    
    private final String paymentKey;
    private final String orderId;
    
    public DuplicateOrderIdException(String message, String paymentKey, String orderId, Throwable cause) {
        super(message, cause);
        this.paymentKey = paymentKey;
        this.orderId = orderId;
    }
    
    public String getPaymentKey() {
        return paymentKey;
    }
    
    public String getOrderId() {
        return orderId;
    }
}
