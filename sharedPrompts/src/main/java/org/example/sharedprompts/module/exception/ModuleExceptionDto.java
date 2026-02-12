package org.example.sharedprompts.module.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModuleExceptionDto {
    private final String code;
    private final String message;
    private final String field;

    public ModuleExceptionDto(ModuleErrorCode errorCode, String field) {
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.field = field;
    }

    public ModuleExceptionDto(ModuleErrorCode errorCode) {
        this(errorCode, null);
    }

    public static ModuleExceptionDto of(ModuleErrorCode errorCode, String field) {
        return new ModuleExceptionDto(errorCode, field);
    }

    public static ModuleExceptionDto of(ModuleErrorCode errorCode) {
        return new ModuleExceptionDto(errorCode);
    }
}

