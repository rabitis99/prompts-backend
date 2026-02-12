package org.example.sharedprompts.module.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import org.springframework.http.HttpStatus;

public record ModuleResponse<T>(
        @JsonIgnore
        HttpStatus httpStatus,
        boolean success,
        @Nullable T data,
        @Nullable ModuleExceptionDto error
) {
    public static <T> ModuleResponse<T> ok(@Nullable T data) {
        return new ModuleResponse<>(HttpStatus.OK, true, data, null);
    }

    public static <T> ModuleResponse<T> created(@Nullable T data) {
        return new ModuleResponse<>(HttpStatus.CREATED, true, data, null);
    }

    public static <T> ModuleResponse<T> fail(BaseException e) {
        ModuleExceptionDto dto = e.getFieldName() != null
                ? ModuleExceptionDto.of(e.getErrorCode(), e.getFieldName())
                : ModuleExceptionDto.of(e.getErrorCode());
        return new ModuleResponse<>(e.getErrorCode().getHttpStatus(), false, null, dto);
    }
}

