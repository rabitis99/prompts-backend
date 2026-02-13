package org.example.sharedprompts.module.domain.production.service.storage;

import org.slf4j.LoggerFactory;

import java.time.Duration;

public interface StorageStrategy {

    String store(String content, Long userId, String jobId, String fileName);

    String store(byte[] data, String contentType, Long userId, String jobId, String fileName);

    // NOTE: 인터페이스 default 메서드에서는 Logger를 필드에 캐싱할 수 없으므로 호출마다 조회합니다.
    // 이 폴백은 구현체가 tenant-aware store를 오버라이드하지 않은 경우에만 호출되므로 실제 영향은 제한적입니다.
    default String store(String content, String tenantId, Long userId, String jobId, String fileName) {
        LoggerFactory.getLogger(getClass()).warn(
                "Tenant-aware store not implemented by {}. tenantId '{}' will be ignored - data may not be tenant-isolated.",
                getClass().getSimpleName(), tenantId);
        return store(content, userId, jobId, fileName);
    }

    default String store(byte[] data, String contentType, String tenantId, Long userId, String jobId, String fileName) {
        LoggerFactory.getLogger(getClass()).warn(
                "Tenant-aware store not implemented by {}. tenantId '{}' will be ignored - data may not be tenant-isolated.",
                getClass().getSimpleName(), tenantId);
        return store(data, contentType, userId, jobId, fileName);
    }

    String generateChecksum(String content);

    StorageType getStorageType();

    default byte[] read(String storagePath) {
        throw new UnsupportedOperationException(
                "read() is not supported by " + getClass().getSimpleName());
    }

    default boolean exists(String storagePath) {
        throw new UnsupportedOperationException(
                "exists() is not supported by " + getClass().getSimpleName());
    }

    default void delete(String storagePath) {
        throw new UnsupportedOperationException(
                "delete() is not supported by " + getClass().getSimpleName());
    }

    default String generateAccessUrl(String storagePath, Duration ttl) {
        throw new UnsupportedOperationException(
                "generateAccessUrl() is not supported by " + getClass().getSimpleName());
    }
}
