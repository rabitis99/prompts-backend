package org.example.sharedprompts.dto.common;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.dto.ExceptionDto;
import org.springframework.http.HttpStatus;

public record CustomResponse<T>(
        @JsonIgnore
        HttpStatus httpStatus,
        boolean success,
        @Nullable T data,
        @Nullable ExceptionDto error
) {

    public static <T> CustomResponse<T> ok(@Nullable final T data) {
        return new CustomResponse<>(HttpStatus.OK, true, data, null);
    }

    public static <T> CustomResponse<T> created(@Nullable final T data) {
        return new CustomResponse<>(HttpStatus.CREATED, true, data, null);
    }

    public static <T> CustomResponse<T> fail(final ApiException e) {
        ExceptionDto dto = e.getFieldName() != null
                ? ExceptionDto.of(e.getErrorCode(), e.getFieldName())
                : ExceptionDto.of(e.getErrorCode());
        return new CustomResponse<>(e.getErrorCode().getHttpStatus(), false, null, dto);
    }
}

