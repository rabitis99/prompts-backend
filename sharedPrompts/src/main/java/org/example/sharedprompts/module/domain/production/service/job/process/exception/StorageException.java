package org.example.sharedprompts.module.domain.production.service.job.process.exception;

import org.example.sharedprompts.module.exception.ModuleErrorCode;

import static org.example.sharedprompts.module.exception.ModuleErrorCode.STORAGE_ERROR;

public class StorageException extends JobProcessingException {
    
    public StorageException(String message) {
        super(STORAGE_ERROR, message);
    }

    public StorageException(String message, Throwable cause) {
        super(STORAGE_ERROR, message, cause);
    }

    public StorageException(ModuleErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public StorageException(ModuleErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}

