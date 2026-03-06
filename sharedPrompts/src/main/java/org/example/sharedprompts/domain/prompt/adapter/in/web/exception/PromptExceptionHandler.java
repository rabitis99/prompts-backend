package org.example.sharedprompts.domain.prompt.adapter.in.web.exception;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.application.exception.InvalidPromptUpdateException;
import org.example.sharedprompts.domain.prompt.application.exception.PromptAccessDeniedException;
import org.example.sharedprompts.domain.prompt.application.exception.PromptNotFoundException;
import org.example.sharedprompts.domain.prompt.application.exception.UnsupportedQualityPipelineOptionException;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Prompt 도메인 전용 예외 핸들러.
 *
 * <p>애플리케이션 계층의 도메인 예외를 웹 전용 ApiException/ErrorCode 로 매핑한다.</p>
 */
@Slf4j
@RestControllerAdvice(basePackages = "org.example.sharedprompts.domain.prompt")
public class PromptExceptionHandler {

    @ExceptionHandler(PromptNotFoundException.class)
    public ResponseEntity<CustomResponse<Void>> handleNotFound(PromptNotFoundException ex) {
        log.warn("PromptNotFoundException: {}", ex.getMessage());
        ApiException apiEx = new ApiException(ErrorCode.PROMPT_NOT_FOUND, ex.getMessage());
        return ResponseEntity.status(apiEx.getErrorCode().getHttpStatus())
                .body(CustomResponse.fail(apiEx));
    }

    @ExceptionHandler(PromptAccessDeniedException.class)
    public ResponseEntity<CustomResponse<Void>> handleAccessDenied(PromptAccessDeniedException ex) {
        log.warn("PromptAccessDeniedException: {}", ex.getMessage());
        ApiException apiEx = new ApiException(ErrorCode.PROMPT_FORBIDDEN, ex.getMessage());
        return ResponseEntity.status(apiEx.getErrorCode().getHttpStatus())
                .body(CustomResponse.fail(apiEx));
    }

    @ExceptionHandler(InvalidPromptUpdateException.class)
    public ResponseEntity<CustomResponse<Void>> handleInvalidUpdate(InvalidPromptUpdateException ex) {
        log.warn("InvalidPromptUpdateException: {}", ex.getMessage());
        ApiException apiEx = new ApiException(ErrorCode.INVALID_INPUT_VALUE, ex.getMessage());
        return ResponseEntity.status(apiEx.getErrorCode().getHttpStatus())
                .body(CustomResponse.fail(apiEx));
    }

    @ExceptionHandler(UnsupportedQualityPipelineOptionException.class)
    public ResponseEntity<CustomResponse<Void>> handleUnsupportedQuality(UnsupportedQualityPipelineOptionException ex) {
        log.warn("UnsupportedQualityPipelineOptionException: {}", ex.getMessage());
        ApiException apiEx = new ApiException(ErrorCode.UNSUPPORTED_QUALITY_PIPELINE_OPTION, ex.getMessage());
        return ResponseEntity.status(apiEx.getErrorCode().getHttpStatus())
                .body(CustomResponse.fail(apiEx));
    }
}

