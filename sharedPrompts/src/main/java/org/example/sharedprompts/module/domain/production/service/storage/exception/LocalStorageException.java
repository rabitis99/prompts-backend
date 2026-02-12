package org.example.sharedprompts.module.domain.production.service.storage.exception;

import org.example.sharedprompts.module.exception.ModuleErrorCode;

/**
 * 로컬 저장소 전략에서 발생하는 예외.
 */
public class LocalStorageException extends AbstractStorageException {
    
    public LocalStorageException(String message) {
        super(message);
    }

    public LocalStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public LocalStorageException(ModuleErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public LocalStorageException(ModuleErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}

