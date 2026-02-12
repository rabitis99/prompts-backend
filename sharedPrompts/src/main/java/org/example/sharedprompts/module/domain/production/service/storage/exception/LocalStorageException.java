package org.example.sharedprompts.module.domain.production.service.storage.exception;

import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;

import static org.example.sharedprompts.module.exception.ModuleErrorCode.STORAGE_ERROR;

public class LocalStorageException extends BaseException {
    
    public LocalStorageException(String message) {
        super(STORAGE_ERROR, null, message);
    }

    public LocalStorageException(String message, Throwable cause) {
        super(STORAGE_ERROR, null, message, cause);
    }

    public LocalStorageException(ModuleErrorCode errorCode, String message) {
        super(errorCode, null, message);
    }

    public LocalStorageException(ModuleErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, null, message, cause);
    }
}

