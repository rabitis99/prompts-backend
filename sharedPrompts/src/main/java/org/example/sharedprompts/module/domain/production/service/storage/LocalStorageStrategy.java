package org.example.sharedprompts.module.domain.production.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.util.ChecksumUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@Slf4j
public class LocalStorageStrategy implements StorageStrategy {

    @Value("${production.storage.local.base-path:./storage/production}")
    private String basePath;

    @Override
    public String store(String content, Long userId, String jobId, String fileName) {
        log.info("Storing file locally - userId: {}, jobId: {}, fileName: {}", 
                userId, jobId, fileName);

        try {
            // 경로 생성: basePath/userId/jobId/fileName
            Path directory = Paths.get(basePath, String.valueOf(userId), jobId);
            Files.createDirectories(directory);

            Path filePath = directory.resolve(fileName);
            Files.writeString(filePath, content);

            log.info("File stored successfully - path: {}", filePath);

            return filePath.toString();

        } catch (IOException e) {
            log.error("Failed to store file locally - userId: {}, jobId: {}, fileName: {}", 
                    userId, jobId, fileName, e);
            throw new StorageException("Failed to store file: " + e.getMessage(), e);
        }
    }

    @Override
    public String store(byte[] data, String contentType, Long userId, String jobId, String fileName) {
        log.info("Storing binary file locally - userId: {}, jobId: {}, fileName: {}, contentType: {}",
                userId, jobId, fileName, contentType);

        try {
            Path directory = Paths.get(basePath, String.valueOf(userId), jobId);
            Files.createDirectories(directory);

            Path filePath = directory.resolve(fileName);
            Files.write(filePath, data);

            log.info("Binary file stored successfully - path: {}, size: {} bytes", filePath, data.length);

            return filePath.toString();

        } catch (IOException e) {
            log.error("Failed to store binary file locally - userId: {}, jobId: {}, fileName: {}",
                    userId, jobId, fileName, e);
            throw new StorageException("Failed to store binary file: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateChecksum(String content) {
        return ChecksumUtils.generateSha256(content);
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.LOCAL;
    }

    /**
     * 저장 예외
     */
    public static class StorageException extends RuntimeException {
        public StorageException(String message) {
            super(message);
        }

        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

