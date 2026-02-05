package org.example.sharedprompts.domain.payment.domain.exception;

import org.example.sharedprompts.global.exception.ErrorCode;

/**
 * PaymentMethod 관련 예외
 * 
 * <p>PaymentMethod enum 변환 실패 시 사용하는 도메인 예외입니다.
 * PaymentDomainException을 상속받아 일관된 예외 처리를 제공합니다.
 */
public class PaymentMethodException extends PaymentDomainException {
    
    /**
     * PaymentMethodException 생성
     * 
     * @param message 예외 메시지
     */
    public PaymentMethodException(String message) {
        super(ErrorCode.PAYMENT_PROVIDER_ERROR, "payment_method", message);
    }

    /**
     * PaymentMethodException 생성 (원인 포함)
     * 
     * @param message 예외 메시지
     * @param cause 원인 예외
     */
    public PaymentMethodException(String message, Throwable cause) {
        super(ErrorCode.PAYMENT_PROVIDER_ERROR, "payment_method", message, cause);
    }
}








