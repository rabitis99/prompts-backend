package org.example.sharedprompts.module.domain.production.service.job.idempotencykey.exception;

/**
 * 멱등성 키 생성 중 발생하는 예외
 */
public class IdempotencyKeyGenerationException extends RuntimeException {

    public IdempotencyKeyGenerationException(String message) {
        super(message);
    }

    public IdempotencyKeyGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}

