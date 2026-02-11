package org.example.sharedprompts.module.exception;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.module.domain.production.application.exception.ProductionApplicationException;
import org.example.sharedprompts.module.domain.production.application.exception.ProductionCommandFactoryNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Production 모듈 전용 예외 핸들러
 */
@RestControllerAdvice(basePackages = "org.example.sharedprompts.module")
@Slf4j
public class ModuleExceptionHandler {

    /**
     * ProductionCommandFactoryNotFoundException 처리
     * 지원하지 않는 Production 타입 요청 시 발생
     */
    @ExceptionHandler(ProductionCommandFactoryNotFoundException.class)
    public ResponseEntity<?> handleProductionCommandFactoryNotFoundException(
            ProductionCommandFactoryNotFoundException e) {
        log.warn("ProductionCommandFactory not found: {}", e.getMessage());
        return CustomResponseHelper.fail(new ApiException(ErrorCode.INVALID_INPUT_VALUE, 
                "지원하지 않는 Production 타입입니다."));
    }

    /**
     * ProductionApplicationException 처리
     * Application 계층에서 발생하는 일반적인 예외
     */
    @ExceptionHandler(ProductionApplicationException.class)
    public ResponseEntity<?> handleProductionApplicationException(
            ProductionApplicationException e) {
        log.error("Production application error: {}", e.getMessage(), e);
        return CustomResponseHelper.fail(new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, 
                "Production 처리 중 오류가 발생했습니다."));
    }

    /**
     * IllegalStateException 처리
     * Job 상태 전이 오류, 복구 불가능한 상태 등에서 발생
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalStateException(IllegalStateException e) {
        log.warn("IllegalStateException in Production module: {}", e.getMessage());
        
        // Job 상태 관련 오류인지 확인
        String message = e.getMessage();
        if (message != null && (
                message.contains("Cannot") && message.contains("status") ||
                message.contains("Job") && message.contains("state")
        )) {
            return CustomResponseHelper.fail(new ApiException(ErrorCode.INVALID_INPUT_VALUE, 
                    "Job 상태 오류: " + message));
        }
        
        // 일반적인 IllegalStateException
        return CustomResponseHelper.fail(new ApiException(ErrorCode.INVALID_INPUT_VALUE, 
                "요청을 처리할 수 없는 상태입니다: " + message));
    }
}
