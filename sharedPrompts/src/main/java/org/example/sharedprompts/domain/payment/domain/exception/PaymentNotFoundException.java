package org.example.sharedprompts.domain.payment.domain.exception;

/**
 * 결제를 찾을 수 없을 때 발생하는 예외
 */
public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String message) {
        super(message);
    }

    public PaymentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
