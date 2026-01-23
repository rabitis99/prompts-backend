package org.example.sharedprompts.global.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String fieldName;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.fieldName = null;
    }

    public ApiException(ErrorCode errorCode, String fieldName) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.fieldName = fieldName;
    }

    public ApiException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.fieldName = null;
    }

    public ApiException(ErrorCode errorCode, String fieldName, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.fieldName = fieldName;
    }

    public ApiException(ErrorCode errorCode, String fieldName, String customMessage) {
        super(getMessage(customMessage, errorCode));
        this.errorCode = errorCode;
        this.fieldName = fieldName;
    }

    public ApiException(ErrorCode errorCode, String fieldName, String customMessage, Throwable cause) {
        super(getMessage(customMessage, errorCode), cause);
        this.errorCode = errorCode;
        this.fieldName = fieldName;
    }

    private static String getMessage(String customMessage, ErrorCode errorCode) {
        return customMessage != null && !customMessage.isEmpty() ? customMessage : errorCode.getMessage();
    }
}