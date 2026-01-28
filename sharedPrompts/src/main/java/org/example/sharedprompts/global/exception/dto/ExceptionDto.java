package org.example.sharedprompts.global.exception.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.example.sharedprompts.global.exception.ErrorCode;

import java.util.Map;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExceptionDto {

    @NotNull
    private final String code;

    @NotNull
    private final String message;

    private final String field;

    private final Map<String, Object> details;

    public ExceptionDto(ErrorCode errorCode, String field, Map<String, Object> details) {
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.field = field;
        this.details = details;
    }

    public ExceptionDto(ErrorCode errorCode, String field) {
        this(errorCode, field, null);
    }

    public ExceptionDto(ErrorCode errorCode) {
        this(errorCode, null, null);
    }

    public static ExceptionDto of(ErrorCode errorCode, String field, Map<String, Object> details) {
        return new ExceptionDto(errorCode, field, details);
    }

    public static ExceptionDto of(ErrorCode errorCode, String field) {
        return new ExceptionDto(errorCode, field);
    }

    public static ExceptionDto of(ErrorCode errorCode) {
        return new ExceptionDto(errorCode);
    }
}
