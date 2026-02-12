package org.example.sharedprompts.module.domain.production.validation;

import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;

import static org.example.sharedprompts.module.exception.ModuleErrorCode.VALIDATION_ERROR;

public class ValidationException extends BaseException {
    
    public ValidationException(String message) {
        super(VALIDATION_ERROR, null, message);
    }

    public ValidationException(String fieldName, String message) {
        super(VALIDATION_ERROR, fieldName, message);
    }

    public ValidationException(String message, Throwable cause) {
        super(VALIDATION_ERROR, null, message, cause);
    }

    public ValidationException(ModuleErrorCode errorCode, String message) {
        super(errorCode, null, message);
    }

    public ValidationException(ModuleErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, null, message, cause);
    }
}


