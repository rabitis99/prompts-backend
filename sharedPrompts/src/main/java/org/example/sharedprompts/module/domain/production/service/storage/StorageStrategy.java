package org.example.sharedprompts.module.domain.production.service.storage;

import java.time.Duration;

public interface StorageStrategy {

    String store(String content, Long userId, String jobId, String fileName);

    String store(byte[] data, String contentType, Long userId, String jobId, String fileName);

    default String store(String content, String tenantId, Long userId, String jobId, String fileName) {
        return store(content, userId, jobId, fileName);
    }

    default String store(byte[] data, String contentType, String tenantId, Long userId, String jobId, String fileName) {
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
