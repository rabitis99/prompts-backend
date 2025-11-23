package org.example.sharedprompts.global.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String fieldName;

    public ApiException(ErrorCode errorCode, String fieldName) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.fieldName = fieldName;
    }
    public ApiException(ErrorCode errorCode) {
        this(errorCode, null);
    }
}