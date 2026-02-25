package org.example.sharedprompts.domain.payment.application.exception;

/**
 * 결제 애플리케이션 예외
 * 결제 처리 중 발생하는 일반적인 예외
 */
public class PaymentApplicationException extends RuntimeException {

    public PaymentApplicationException(String message) {
        super(message);
    }

    public PaymentApplicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public PaymentApplicationException(Throwable cause) {
        super(cause);
    }
}
