package org.example.sharedprompts.global.exception.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.example.sharedprompts.global.exception.ErrorCode;

@Getter
public class ExceptionDto {

    @NotNull
    private final String code;

    @NotNull
    private final String message;

    private final String field;

    public ExceptionDto(ErrorCode errorCode, String field) {
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.field = field;
    }

    public ExceptionDto(ErrorCode errorCode) {
        this(errorCode, null);
    }

    public static ExceptionDto of(ErrorCode errorCode, String field) {
        return new ExceptionDto(errorCode, field);
    }

    public static ExceptionDto of(ErrorCode errorCode) {
        return new ExceptionDto(errorCode);
    }
}
