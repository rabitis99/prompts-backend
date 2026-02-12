package org.example.sharedprompts.module.domain.production.service.job.process.exception;

import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;

public class JobProcessingException extends BaseException {
    
    public JobProcessingException(ModuleErrorCode errorCode) {
        super(errorCode);
    }

    public JobProcessingException(ModuleErrorCode errorCode, String message) {
        super(errorCode, null, message);
    }

    public JobProcessingException(ModuleErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public JobProcessingException(ModuleErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, null, message, cause);
    }
}

