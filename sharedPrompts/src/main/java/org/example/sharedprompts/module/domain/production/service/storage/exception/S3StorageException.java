package org.example.sharedprompts.module.domain.production.service.storage.exception;

import org.example.sharedprompts.module.exception.ModuleErrorCode;

/**
 * S3 저장소 전략에서 발생하는 예외.
 */
public class S3StorageException extends AbstractStorageException {
    
    public S3StorageException(String message) {
        super(message);
    }

    public S3StorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public S3StorageException(ModuleErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public S3StorageException(ModuleErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}

