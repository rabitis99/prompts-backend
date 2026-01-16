package org.example.sharedprompts.global.exception;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.response.CustomResponseHelper;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
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

    // 지원되지 않는 HTTP 메서드 예외 처리
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

    // DB 제약 조건 위반 예외 처리 (UNIQUE 제약 등)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String message = e.getMessage();
        log.warn("DataIntegrityViolationException: {}", message);
        
        // ConstraintViolationException의 constraint name을 기준으로 매핑 (더 안정적)
        Throwable cause = e.getCause();
        if (cause instanceof ConstraintViolationException cve) {
            String constraintName = cve.getConstraintName();
            if (constraintName != null) {
                ErrorCode errorCode = mapToDomainErrorCodeByConstraint(constraintName);
                return CustomResponseHelper.fail(new ApiException(errorCode));
            }
        }
        
        // constraint name을 찾을 수 없는 경우 메시지 기반 매핑 (fallback)
        if (message != null) {
            ErrorCode errorCode = mapToDomainErrorCodeByMessage(message);
            return CustomResponseHelper.fail(new ApiException(errorCode));
        }
        
        return CustomResponseHelper.fail(new ApiException(ErrorCode.DATA_INTEGRITY_VIOLATION));
    }
    
    /**
     * 제약 조건 이름을 기준으로 도메인별 ErrorCode로 매핑 (권장 방식)
     */
    private ErrorCode mapToDomainErrorCodeByConstraint(String constraintName) {
        // 신고 중복 제약 조건 위반
        if ("uk_report_prompt_reporter".equalsIgnoreCase(constraintName) || 
            "uk_report_comment_reporter".equalsIgnoreCase(constraintName)) {
            return ErrorCode.REPORT_ALREADY_EXISTS;
        }
        
        // 사용자 중복 제약 조건 위반 (provider, providerId)
        // User 엔티티에 명시적 constraint name이 없어 Hibernate가 자동 생성하므로
        // constraint name에 provider와 providerId가 포함된 경우로 판단
        if (constraintName != null) {
            String lowerConstraintName = constraintName.toLowerCase();
            if ((lowerConstraintName.contains("provider") && lowerConstraintName.contains("providerid")) ||
                lowerConstraintName.contains("users_provider_providerid")) {
                return ErrorCode.CONFLICT_EMAIL;
            }
        }
        
        // 태그 이름 중복 제약 조건 위반
        if ("uk_tag_name".equalsIgnoreCase(constraintName)) {
            return ErrorCode.DATA_INTEGRITY_VIOLATION; // 또는 TAG_ALREADY_EXISTS ErrorCode 추가 가능
        }
        
        // 프롬프트-태그 중복 제약 조건 위반
        if ("uk_prompt_tag".equalsIgnoreCase(constraintName)) {
            return ErrorCode.DATA_INTEGRITY_VIOLATION; // 또는 PROMPT_TAG_ALREADY_EXISTS ErrorCode 추가 가능
        }
        
        // 기타 제약 조건 위반
        return ErrorCode.DATA_INTEGRITY_VIOLATION;
    }
    
    /**
     * 제약 조건 위반 메시지를 도메인별 ErrorCode로 매핑 (fallback)
     */
    private ErrorCode mapToDomainErrorCodeByMessage(String message) {
        // 신고 중복 제약 조건 위반
        if (message.contains("uk_report_prompt_reporter") || 
            message.contains("uk_report_comment_reporter") ||
            (message.contains("reporter_id") && message.contains("prompt_id")) ||
            (message.contains("reporter_id") && message.contains("comment_id"))) {
            return ErrorCode.REPORT_ALREADY_EXISTS;
        }
        
        // 사용자 중복 제약 조건 위반 (provider, providerId)
        if (message.contains("provider") && message.contains("providerId")) {
            return ErrorCode.CONFLICT_EMAIL;
        }
        
        // 태그 이름 중복 제약 조건 위반
        if (message.contains("uk_tag_name") || (message.contains("tags") && message.contains("name"))) {
            return ErrorCode.DATA_INTEGRITY_VIOLATION; // 또는 TAG_ALREADY_EXISTS ErrorCode 추가 가능
        }
        
        // 프롬프트-태그 중복 제약 조건 위반
        if (message.contains("prompt_id") && message.contains("tag_id")) {
            return ErrorCode.DATA_INTEGRITY_VIOLATION; // 또는 PROMPT_TAG_ALREADY_EXISTS ErrorCode 추가 가능
        }
        
        // 기타 제약 조건 위반
        return ErrorCode.DATA_INTEGRITY_VIOLATION;
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