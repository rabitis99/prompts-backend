package org.example.sharedprompts.module.exception;

import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {
    private final ModuleErrorCode errorCode;
    private final String fieldName;

    public BaseException(ModuleErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.fieldName = null;
    }

    public BaseException(ModuleErrorCode errorCode, String fieldName) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.fieldName = fieldName;
    }

    public BaseException(ModuleErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.fieldName = null;
    }

    public BaseException(ModuleErrorCode errorCode, String fieldName, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.fieldName = fieldName;
    }

    public BaseException(ModuleErrorCode errorCode, String fieldName, String customMessage) {
        super(customMessage != null && !customMessage.isEmpty() ? customMessage : errorCode.getMessage());
        this.errorCode = errorCode;
        this.fieldName = fieldName;
    }

    public BaseException(ModuleErrorCode errorCode, String fieldName, String customMessage, Throwable cause) {
        super(customMessage != null && !customMessage.isEmpty() ? customMessage : errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.fieldName = fieldName;
    }
}

