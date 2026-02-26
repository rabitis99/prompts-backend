package org.example.sharedprompts.domain.payment.domain.exception;

/**
 * 결제 검증에 실패할 때 발생하는 예외
 */
public class PaymentValidationException extends RuntimeException {

    public PaymentValidationException(String message) {
        super(message);
    }

    public PaymentValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
