package org.example.sharedprompts.module.domain.production.validation;

/**
 * 검증 실패 예외
 */
public class ValidationException extends RuntimeException {
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}


