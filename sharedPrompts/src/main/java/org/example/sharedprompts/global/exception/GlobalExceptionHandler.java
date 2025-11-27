package org.example.sharedprompts.global.exception;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.response.CustomResponseHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 존재하지 않는 요청(엔드포인트) 처리
    @ExceptionHandler({NoResourceFoundException.class})
    public ResponseEntity<?> handleNoPageFoundException(NoResourceFoundException e) {
        log.warn("NoResourceFoundException: {}", e.getMessage());
        return CustomResponseHelper.fail(new ApiException(ErrorCode.NOT_FOUND_ENDPOINT));
    }

    //지원되지 않는 HTTP 메서드 예외 처리
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
     public ResponseEntity<?> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("HttpRequestMethodNotSupportedException: {}", e.getMessage());
        return CustomResponseHelper.fail(new ApiException(ErrorCode.METHOD_NOT_ALLOWED));
    }

    //유효성 검사 실패(@Valid, @Validated) 예외 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String fieldName = fieldError != null ? fieldError.getField() : null;
        String message = fieldError != null ? fieldError.getDefaultMessage() : "Validation failed";
        log.warn("Validation failed: {} ({})", message, fieldName);
        return CustomResponseHelper.fail(new ApiException(ErrorCode.INVALID_INPUT_VALUE, fieldName));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<?> handleJwtException(JwtException e) {
        log.warn("JWT 인증 실패: {}", e.getMessage());
        return CustomResponseHelper.fail(new ApiException(ErrorCode.UNAUTHORIZED));
    }
    // 커스텀 예외
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handleCustomException(ApiException e) {
        log.error("ApiException caught: {} ({})", e.getMessage(), e.getErrorCode().name());
        return CustomResponseHelper.fail(e);
    }

    // 기본 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e) {
        log.error("Unexpected exception caught: {}", e.getMessage(), e);
        return CustomResponseHelper.fail(new ApiException(ErrorCode.INTERNAL_SERVER_ERROR));
    }

}