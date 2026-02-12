package org.example.sharedprompts.module.exception;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.application.exception.ProductionCommandFactoryNotFoundException;
import org.example.sharedprompts.module.domain.production.validation.ValidationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Module 패키지 내 예외를 처리하는 전역 예외 핸들러.
 * 
 * 대부분의 BaseException 하위 타입은 handleBaseException에서 통합 처리되며,
 * warn 레벨 로깅이 필요한 예외만 개별 핸들러로 처리합니다.
 */
@RestControllerAdvice(basePackages = "org.example.sharedprompts.module")
@Slf4j
public class ModuleExceptionHandler {

    /**
     * BaseException 및 모든 하위 타입을 처리하는 통합 핸들러.
     * 예외 클래스 이름을 로그에 포함하여 디버깅을 용이하게 합니다.
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ModuleResponse<Void>> handleBaseException(BaseException e) {
        log.error("{}: {} ({})", e.getClass().getSimpleName(), e.getMessage(), e.getErrorCode().name(), e);
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ModuleResponse.fail(e));
    }

    /**
     * ProductionCommandFactoryNotFoundException은 warn 레벨로 처리합니다.
     */
    @ExceptionHandler(ProductionCommandFactoryNotFoundException.class)
    public ResponseEntity<ModuleResponse<Void>> handleProductionCommandFactoryNotFoundException(
            ProductionCommandFactoryNotFoundException e) {
        log.warn("ProductionCommandFactory not found: {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ModuleResponse.fail(e));
    }

    /**
     * ValidationException은 warn 레벨로 처리합니다.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ModuleResponse<Void>> handleValidationException(ValidationException e) {
        log.warn("Validation error: {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ModuleResponse.fail(e));
    }

    /**
     * IllegalStateException을 처리합니다.
     * 
     * TODO: 이 핸들러는 예외 메시지 문자열 매칭에 의존하므로 깨지기 쉽습니다.
     * 가능하다면 IllegalStateException 대신 처음부터 적절한 BaseException 하위 타입
     * (예: JobInvalidStatusException)을 던지도록 발생 지점을 리팩터링하는 것이 더 안전합니다.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ModuleResponse<Void>> handleIllegalStateException(IllegalStateException e) {
        log.warn("IllegalStateException: {}", e.getMessage());
        String message = e.getMessage();
        BaseException baseException;
        if (message != null && (
                message.contains("Cannot") && message.contains("status") ||
                message.contains("Job") && message.contains("state")
        )) {
            baseException = new BaseException(
                    ModuleErrorCode.JOB_INVALID_STATUS,
                    null,
                    "Job 상태 오류: " + message
            );
        } else {
            baseException = new BaseException(
                    ModuleErrorCode.VALIDATION_ERROR,
                    null,
                    "요청을 처리할 수 없는 상태입니다: " + message
            );
        }
        return ResponseEntity
                .status(baseException.getErrorCode().getHttpStatus())
                .body(ModuleResponse.fail(baseException));
    }
}
