package org.example.sharedprompts.module.exception.translator;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.infra.storage.exception.S3StorageException;
import org.example.sharedprompts.module.domain.production.service.ai.exception.AiClientException;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.springframework.stereotype.Component;

/**
 * module 영역의 표준 예외 매핑 구현체.
 */
@Component
@Slf4j
public class ModuleExceptionTranslator implements ExceptionTranslator {

    @Override
    public BaseException translate(Throwable t) {
        if (t == null) {
            return new BaseException(ModuleErrorCode.RECOVERY_ERROR, null, "Unknown error");
        }

        if (t instanceof BaseException be) {
            return be;
        }

        if (t instanceof S3StorageException) {
            return new BaseException(ModuleErrorCode.STORAGE_ERROR, t);
        }

        if (t instanceof AiClientException) {
            return new BaseException(ModuleErrorCode.AI_CLIENT_ERROR, t);
        }

        if (t instanceof CallNotPermittedException) {
            // circuit breaker OPEN: 즉시 실패(return)하도록 명확한 코드로 매핑
            return new BaseException(ModuleErrorCode.AI_SERVICE_ERROR, null, "AI service is temporarily unavailable", t);
        }

        if (t instanceof IllegalArgumentException) {
            return new BaseException(ModuleErrorCode.VALIDATION_ERROR, null, t.getMessage(), t);
        }

        if (t instanceof IllegalStateException) {
            // 내부 상태/설정 오류에 해당하는 경우가 대부분이므로 5xx로 매핑합니다.
            // (정상적인 비즈니스/검증 오류는 BaseException/JobProcessingException으로 던지도록 유도)
            return new BaseException(ModuleErrorCode.RECOVERY_ERROR, null, t.getMessage(), t);
        }

        // P1-5: Unknown 예외는 STORAGE_ERROR가 아닌 RECOVERY_ERROR(INTERNAL_ERROR)로 매핑
        // STORAGE_ERROR는 실제 storage 관련 예외에만 사용하고, 알 수 없는 예외는 내부 오류로 처리
        log.warn("Unhandled exception type in ModuleExceptionTranslator: {}", t.getClass().getName());
        return new BaseException(ModuleErrorCode.RECOVERY_ERROR, t);
    }
}
