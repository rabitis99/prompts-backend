package org.example.sharedprompts.module.domain.production.application.exception;

/**
 * Production Application 계층에서 발생하는 예외의 기본 클래스
 */
public class ProductionApplicationException extends RuntimeException {
    
    public ProductionApplicationException(String message) {
        super(message);
    }
    
    public ProductionApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}




