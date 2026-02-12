package org.example.sharedprompts.module.domain.production.service.storage.exception;

import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;

import static org.example.sharedprompts.module.exception.ModuleErrorCode.STORAGE_ERROR;

/**
 * 저장소 관련 예외의 공통 추상 클래스.
 * LocalStorageException과 S3StorageException의 중복 코드를 제거합니다.
 */
public abstract class AbstractStorageException extends BaseException {
    
    public AbstractStorageException(String message) {
        super(STORAGE_ERROR, null, message);
    }

    public AbstractStorageException(String message, Throwable cause) {
        super(STORAGE_ERROR, null, message, cause);
    }

    public AbstractStorageException(ModuleErrorCode errorCode, String message) {
        super(errorCode, null, message);
    }

    public AbstractStorageException(ModuleErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, null, message, cause);
    }
}

