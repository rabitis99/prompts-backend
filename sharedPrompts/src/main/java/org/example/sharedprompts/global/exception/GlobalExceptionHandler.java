package org.example.sharedprompts.global.exception;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.response.CustomResponseHelper;
import jakarta.persistence.OptimisticLockException;
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

    // 낙관적 락 예외 처리 (동시 수정 감지)
    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<?> handleOptimisticLockException(OptimisticLockException e) {
        Object entity = e.getEntity();
        String entityType = entity != null ? entity.getClass().getSimpleName() : "unknown";
        log.warn("OptimisticLockException: 동시 수정이 감지되었습니다. (entityType={})", entityType);
        
        // 엔티티 타입에 따라 적절한 에러 코드 반환
        if (entity != null && entity.getClass().getSimpleName().equals("Report")) {
            return CustomResponseHelper.fail(new ApiException(ErrorCode.REPORT_ALREADY_PROCESSED));
        }
        return CustomResponseHelper.fail(new ApiException(ErrorCode.DATA_INTEGRITY_VIOLATION));
    }

    // DB 제약 조건 위반 예외 처리 (UNIQUE 제약 등)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String message = e.getMessage();
        String constraintName = null;
        Throwable cause = e.getCause();
        if (cause instanceof ConstraintViolationException cve) {
            constraintName = cve.getConstraintName();
        }
        log.warn("DataIntegrityViolationException (constraint={})", constraintName);
        
        // ConstraintViolationException의 constraint name을 기준으로 매핑 (더 안정적)
        if (constraintName != null) {
            ErrorCode errorCode = mapToDomainErrorCodeByConstraint(constraintName);
            return CustomResponseHelper.fail(new ApiException(errorCode));
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
        // 좋아요 중복 제약 조건 위반 (프롬프트)
        if ("uk_prompt_like_prompt_user".equalsIgnoreCase(constraintName)) {
            return ErrorCode.PROMPT_ALREADY_LIKED;
        }

        // 좋아요 중복 제약 조건 위반 (댓글)
        if ("uk_comment_like_comment_user".equalsIgnoreCase(constraintName)) {
            return ErrorCode.COMMENT_ALREADY_LIKED;
        }

        // 신고 중복 제약 조건 위반
        if ("uk_report_prompt_reporter".equalsIgnoreCase(constraintName) || 
            "uk_report_comment_reporter".equalsIgnoreCase(constraintName)) {
            return ErrorCode.REPORT_ALREADY_EXISTS;
        }
        
        // 사용자 중복 제약 조건 위반 (provider, providerId)
        // User 엔티티에 명시적 constraint name이 없어 Hibernate가 자동 생성하므로
        // constraint name에 provider와 providerId가 포함된 경우로 판단
        String lowerConstraintName = constraintName.toLowerCase();
        if ((lowerConstraintName.contains("provider") && lowerConstraintName.contains("providerid")) ||
            lowerConstraintName.contains("users_provider_providerid")) {
            return ErrorCode.CONFLICT_EMAIL;
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
        String lower = message.toLowerCase();
        // 신고 중복 제약 조건 위반
        if (lower.contains("uk_report_prompt_reporter") || 
            lower.contains("uk_report_comment_reporter") ||
            (lower.contains("reporter_id") && lower.contains("prompt_id")) ||
            (lower.contains("reporter_id") && lower.contains("comment_id"))) {
            return ErrorCode.REPORT_ALREADY_EXISTS;
        }
        
        // 사용자 중복 제약 조건 위반 (provider, providerId)
        if (lower.contains("provider") && (lower.contains("providerid") || lower.contains("provider_id"))) {
            return ErrorCode.CONFLICT_EMAIL;
        }
        
        // 태그 이름 중복 제약 조건 위반
        if (lower.contains("uk_tag_name") || (lower.contains("tags") && lower.contains("name"))) {
            return ErrorCode.DATA_INTEGRITY_VIOLATION; // 또는 TAG_ALREADY_EXISTS ErrorCode 추가 가능
        }
        
        // 프롬프트-태그 중복 제약 조건 위반
        if (lower.contains("prompt_id") && lower.contains("tag_id")) {
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