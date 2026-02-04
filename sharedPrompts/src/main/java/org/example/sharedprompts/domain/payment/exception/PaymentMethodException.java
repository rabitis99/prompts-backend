package org.example.sharedprompts.domain.payment.exception;

/**
 * PaymentMethod 관련 예외
 * PaymentMethod enum 변환 실패 시 사용
 */
public class PaymentMethodException extends IllegalArgumentException {
    public PaymentMethodException(String message) {
        super(message);
    }

    public PaymentMethodException(String message, Throwable cause) {
        super(message, cause);
    }
}









