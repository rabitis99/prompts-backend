package org.example.sharedprompts.module.domain.production.service.job.process.exception;

import org.example.sharedprompts.module.exception.ModuleErrorCode;

import static org.example.sharedprompts.module.exception.ModuleErrorCode.AI_SERVICE_ERROR;

public class AIServiceException extends JobProcessingException {
    
    public AIServiceException(String message) {
        super(AI_SERVICE_ERROR, message);
    }

    public AIServiceException(String message, Throwable cause) {
        super(AI_SERVICE_ERROR, message, cause);
    }

    public AIServiceException(ModuleErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public AIServiceException(ModuleErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}

