package org.example.sharedprompts.module.domain.production.service.job.process.exception;

import org.example.sharedprompts.module.exception.ModuleErrorCode;

import static org.example.sharedprompts.module.exception.ModuleErrorCode.RECOVERY_ERROR;

public class RecoveryException extends JobProcessingException {
    
    public RecoveryException(String message) {
        super(RECOVERY_ERROR, message);
    }

    public RecoveryException(String message, Throwable cause) {
        super(RECOVERY_ERROR, message, cause);
    }

    public RecoveryException(ModuleErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public RecoveryException(ModuleErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}

