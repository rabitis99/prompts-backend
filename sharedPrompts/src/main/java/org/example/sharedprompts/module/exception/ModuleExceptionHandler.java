package org.example.sharedprompts.module.exception;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.application.exception.ProductionApplicationException;
import org.example.sharedprompts.module.domain.production.application.exception.ProductionCommandFactoryNotFoundException;
import org.example.sharedprompts.module.domain.production.exception.ParseException;
import org.example.sharedprompts.module.domain.production.service.format.FormatConversionException;
import org.example.sharedprompts.module.domain.production.service.job.idempotencykey.exception.IdempotencyKeyGenerationException;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.AIServiceException;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.ContentRenderException;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.JobProcessingException;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.RecoveryException;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.StorageException;
import org.example.sharedprompts.module.domain.production.service.storage.exception.LocalStorageException;
import org.example.sharedprompts.module.domain.production.service.storage.exception.S3StorageException;
import org.example.sharedprompts.module.domain.production.validation.ValidationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "org.example.sharedprompts.module")
@Slf4j
public class ModuleExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ModuleResponse<Void>> handleBaseException(BaseException e) {
        log.error("BaseException: {} ({})", e.getMessage(), e.getErrorCode().name(), e);
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ModuleResponse.fail(e));
    }

    @ExceptionHandler(ProductionCommandFactoryNotFoundException.class)
    public ResponseEntity<ModuleResponse<Void>> handleProductionCommandFactoryNotFoundException(
            ProductionCommandFactoryNotFoundException e) {
        log.warn("ProductionCommandFactory not found: {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ModuleResponse.fail(e));
    }

    @ExceptionHandler(ProductionApplicationException.class)
    public ResponseEntity<ModuleResponse<Void>> handleProductionApplicationException(
            ProductionApplicationException e) {
        log.error("Production application error: {}", e.getMessage(), e);
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ModuleResponse.fail(e));
    }

    @ExceptionHandler(JobProcessingException.class)
    public ResponseEntity<ModuleResponse<Void>> handleJobProcessingException(JobProcessingException e) {
        log.error("Job processing error: {}", e.getMessage(), e);
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ModuleResponse.fail(e));
    }

    @ExceptionHandler(AIServiceException.class)
    public ResponseEntity<ModuleResponse<Void>> handleAIServiceException(AIServiceException e) {
        log.error("AI service error: {}", e.getMessage(), e);
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ModuleResponse.fail(e));
    }

    @ExceptionHandler(ContentRenderException.class)
    public ResponseEntity<ModuleResponse<Void>> handleContentRenderException(ContentRenderException e) {
        log.error("Content render error: {}", e.getMessage(), e);
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ModuleResponse.fail(e));
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ModuleResponse<Void>> handleStorageException(StorageException e) {
        log.error("Storage error: {}", e.getMessage(), e);
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ModuleResponse.fail(e));
    }

    @ExceptionHandler(LocalStorageException.class)
    public ResponseEntity<ModuleResponse<Void>> handleLocalStorageException(LocalStorageException e) {
        log.error("Local storage error: {}", e.getMessage(), e);
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ModuleResponse.fail(e));
    }

    @ExceptionHandler(S3StorageException.class)
    public ResponseEntity<ModuleResponse<Void>> handleS3StorageException(S3StorageException e) {
        log.error("S3 storage error: {}", e.getMessage(), e);
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ModuleResponse.fail(e));
    }

    @ExceptionHandler(RecoveryException.class)
    public ResponseEntity<ModuleResponse<Void>> handleRecoveryException(RecoveryException e) {
        log.error("Recovery error: {}", e.getMessage(), e);
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ModuleResponse.fail(e));
    }

    @ExceptionHandler(ParseException.class)
    public ResponseEntity<ModuleResponse<Void>> handleParseException(ParseException e) {
        log.error("Parse error: {}", e.getMessage(), e);
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ModuleResponse.fail(e));
    }

    @ExceptionHandler(FormatConversionException.class)
    public ResponseEntity<ModuleResponse<Void>> handleFormatConversionException(FormatConversionException e) {
        log.error("Format conversion error: {}", e.getMessage(), e);
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ModuleResponse.fail(e));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ModuleResponse<Void>> handleValidationException(ValidationException e) {
        log.warn("Validation error: {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ModuleResponse.fail(e));
    }

    @ExceptionHandler(IdempotencyKeyGenerationException.class)
    public ResponseEntity<ModuleResponse<Void>> handleIdempotencyKeyGenerationException(IdempotencyKeyGenerationException e) {
        log.error("Idempotency key generation error: {}", e.getMessage(), e);
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ModuleResponse.fail(e));
    }

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
