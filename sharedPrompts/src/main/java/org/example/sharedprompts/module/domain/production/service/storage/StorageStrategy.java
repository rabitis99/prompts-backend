package org.example.sharedprompts.module.domain.production.service.storage;

import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;

import java.time.Duration;

public interface StorageStrategy {

    String store(String content, Long userId, String jobId, String fileName);

    String store(byte[] data, String contentType, Long userId, String jobId, String fileName);

    /**
     * 테넌트 인식 저장 메서드 (문자열 콘텐츠)
     * 
     * <p>구현체가 이 메서드를 오버라이드하지 않으면 테넌트 격리 위반을 방지하기 위해 예외를 던집니다.
     * 멀티테넌시 환경에서 데이터 격리 위반을 방지하기 위한 fail-fast 전략입니다.
     * 
     * @param content 저장할 콘텐츠
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @param jobId 작업 ID
     * @param fileName 파일명
     * @return 저장된 파일 경로
     * @throws BaseException 테넌트 격리를 지원하지 않는 경우
     */
    default String store(String content, String tenantId, Long userId, String jobId, String fileName) {
        throw new BaseException(
                ModuleErrorCode.STORAGE_ERROR,
                null,
                String.format("Tenant-aware store not implemented by %s. tenantId '%s' cannot be processed - " +
                        "tenant isolation is required for multi-tenant environments. " +
                        "Please implement tenant-aware store methods.",
                        getClass().getSimpleName(), tenantId));
    }

    /**
     * 테넌트 인식 저장 메서드 (바이너리 데이터)
     * 
     * <p>구현체가 이 메서드를 오버라이드하지 않으면 테넌트 격리 위반을 방지하기 위해 예외를 던집니다.
     * 멀티테넌시 환경에서 데이터 격리 위반을 방지하기 위한 fail-fast 전략입니다.
     * 
     * @param data 저장할 바이너리 데이터
     * @param contentType 콘텐츠 타입
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @param jobId 작업 ID
     * @param fileName 파일명
     * @return 저장된 파일 경로
     * @throws BaseException 테넌트 격리를 지원하지 않는 경우
     */
    default String store(byte[] data, String contentType, String tenantId, Long userId, String jobId, String fileName) {
        throw new BaseException(
                ModuleErrorCode.STORAGE_ERROR,
                null,
                String.format("Tenant-aware store not implemented by %s. tenantId '%s' cannot be processed - " +
                        "tenant isolation is required for multi-tenant environments. " +
                        "Please implement tenant-aware store methods.",
                        getClass().getSimpleName(), tenantId));
    }

    String generateChecksum(String content);

    StorageType getStorageType();

    default byte[] read(String storagePath) {
        throw new BaseException(
                ModuleErrorCode.STORAGE_ERROR,
                null,
                "read() is not supported by " + getClass().getSimpleName());
    }

    default boolean exists(String storagePath) {
        throw new BaseException(
                ModuleErrorCode.STORAGE_ERROR,
                null,
                "exists() is not supported by " + getClass().getSimpleName());
    }

    default void delete(String storagePath) {
        throw new BaseException(
                ModuleErrorCode.STORAGE_ERROR,
                null,
                "delete() is not supported by " + getClass().getSimpleName());
    }

    default String generateAccessUrl(String storagePath, Duration ttl) {
        throw new BaseException(
                ModuleErrorCode.STORAGE_ERROR,
                null,
                "generateAccessUrl() is not supported by " + getClass().getSimpleName());
    }
}
