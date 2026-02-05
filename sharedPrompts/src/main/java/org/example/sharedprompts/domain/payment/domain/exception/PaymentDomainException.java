package org.example.sharedprompts.domain.payment.domain.exception;

import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;

/**
 * Payment 도메인 예외
 */
public class PaymentDomainException extends ApiException {

    /**
     * PaymentDomainException 생성
     */
    public PaymentDomainException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * PaymentDomainException 생성 (필드명 포함)
     */
    public PaymentDomainException(ErrorCode errorCode, String fieldName) {
        super(errorCode, fieldName);
    }

    /**
     * PaymentDomainException 생성 (원인 포함)
     */
    public PaymentDomainException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * PaymentDomainException 생성 (필드명 및 원인 포함)
     */
    public PaymentDomainException(ErrorCode errorCode, String fieldName, Throwable cause) {
        super(errorCode, fieldName, cause);
    }

    /**
     * PaymentDomainException 생성 (커스텀 메시지 포함)
     */
    public PaymentDomainException(ErrorCode errorCode, String fieldName, String customMessage) {
        super(errorCode, fieldName, customMessage);
    }

    /**
     * PaymentDomainException 생성 (커스텀 메시지 및 원인 포함)
     */
    public PaymentDomainException(ErrorCode errorCode, String fieldName, String customMessage, Throwable cause) {
        super(errorCode, fieldName, customMessage, cause);
    }
}