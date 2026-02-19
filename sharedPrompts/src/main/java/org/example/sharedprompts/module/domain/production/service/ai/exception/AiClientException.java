package org.example.sharedprompts.module.domain.production.service.ai.exception;

import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;

import static org.example.sharedprompts.module.exception.ModuleErrorCode.AI_CLIENT_ERROR;

/**
 * AI 클라이언트 예외
 * AI 제공자와의 통신 중 발생하는 오류를 래핑
 */
public class AiClientException extends BaseException {
    
    public AiClientException(String message) {
        super(AI_CLIENT_ERROR, null, message);
    }
    
    public AiClientException(String message, Throwable cause) {
        super(AI_CLIENT_ERROR, null, message, cause);
    }
    
    public AiClientException(ModuleErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, null, message, cause);
    }
}

