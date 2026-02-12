package org.example.sharedprompts.module.domain.production.application.exception;

import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;

import static org.example.sharedprompts.module.exception.ModuleErrorCode.AI_SERVICE_ERROR;

public class ProductionApplicationException extends BaseException {
    
    public ProductionApplicationException(String message) {
        super(AI_SERVICE_ERROR, null, message);
    }
    
    public ProductionApplicationException(String message, Throwable cause) {
        super(AI_SERVICE_ERROR, null, message, cause);
    }

    public ProductionApplicationException(ModuleErrorCode errorCode, String message) {
        super(errorCode, null, message);
    }

    public ProductionApplicationException(ModuleErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, null, message, cause);
    }
}





