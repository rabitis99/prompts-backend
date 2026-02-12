package org.example.sharedprompts.module.domain.production.service.job.idempotencykey.exception;

import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;

import static org.example.sharedprompts.module.exception.ModuleErrorCode.IDEMPOTENCY_KEY_ERROR;

public class IdempotencyKeyGenerationException extends BaseException {

    public IdempotencyKeyGenerationException(String message) {
        super(IDEMPOTENCY_KEY_ERROR, null, message);
    }

    public IdempotencyKeyGenerationException(String message, Throwable cause) {
        super(IDEMPOTENCY_KEY_ERROR, null, message, cause);
    }

    public IdempotencyKeyGenerationException(ModuleErrorCode errorCode, String message) {
        super(errorCode, null, message);
    }

    public IdempotencyKeyGenerationException(ModuleErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, null, message, cause);
    }
}

