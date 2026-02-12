package org.example.sharedprompts.module.domain.production.service.storage.exception;

import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;

import static org.example.sharedprompts.module.exception.ModuleErrorCode.STORAGE_ERROR;

public class S3StorageException extends BaseException {
    
    public S3StorageException(String message) {
        super(STORAGE_ERROR, null, message);
    }

    public S3StorageException(String message, Throwable cause) {
        super(STORAGE_ERROR, null, message, cause);
    }

    public S3StorageException(ModuleErrorCode errorCode, String message) {
        super(errorCode, null, message);
    }

    public S3StorageException(ModuleErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, null, message, cause);
    }
}

