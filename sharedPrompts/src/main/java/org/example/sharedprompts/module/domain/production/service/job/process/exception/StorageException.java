package org.example.sharedprompts.module.domain.production.service.job.process.exception;

import org.example.sharedprompts.module.exception.ModuleErrorCode;

import static org.example.sharedprompts.module.exception.ModuleErrorCode.STORAGE_ERROR;

/**
 * 작업 처리 계층에서 발생하는 저장소 예외.
 * 
 * 저장소 관련 예외는 세 가지 계층에서 사용됩니다:
 * <ul>
 *   <li>StorageException (이 클래스) - JobProcessingException을 상속, 작업 처리 계층에서 발생</li>
 *   <li>LocalStorageException - AbstractStorageException을 상속, 로컬 저장소 전략에서 발생</li>
 *   <li>S3StorageException - AbstractStorageException을 상속, S3 저장소 전략에서 발생</li>
 * </ul>
 * 
 * 현재 설계는 계층별로 잘 분리되어 있으며, StorageException은 JobProcessor에서
 * 명시적으로 처리되고, 로컬/S3 예외는 각 전략 계층에서 처리됩니다.
 * 
 * 참고: 모든 저장소 예외가 동일한 STORAGE_ERROR 코드를 사용하므로, 핸들러 레벨에서
 * 구분이 필요하다면 각 예외 타입에 고유한 에러 코드를 할당하는 것을 고려할 수 있습니다.
 */
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

