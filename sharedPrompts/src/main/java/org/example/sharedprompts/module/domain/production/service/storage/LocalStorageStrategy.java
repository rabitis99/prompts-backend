package org.example.sharedprompts.module.domain.production.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.util.ChecksumUtils;
import org.example.sharedprompts.module.domain.production.service.storage.exception.LocalStorageException;
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
            Path directory = Paths.get(basePath, String.valueOf(userId), jobId);
            Files.createDirectories(directory);

            Path filePath = directory.resolve(fileName);
            Files.writeString(filePath, content);

            log.info("File stored successfully - path: {}", filePath);

            return filePath.toString();

        } catch (IOException e) {
            log.error("Failed to store file locally - userId: {}, jobId: {}, fileName: {}", 
                    userId, jobId, fileName, e);
            throw new LocalStorageException("Failed to store file: " + e.getMessage(), e);
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
            throw new LocalStorageException("Failed to store binary file: " + e.getMessage(), e);
        }
    }

    private Path validateAndResolvePath(String storagePath) {
        Path resolved = Paths.get(storagePath).normalize().toAbsolutePath();
        Path base = Paths.get(basePath).normalize().toAbsolutePath();
        if (!resolved.startsWith(base)) {
            throw new LocalStorageException(
                    "Access denied: path is outside storage base directory");
        }
        return resolved;
    }

    @Override
    public byte[] read(String storagePath) {
        log.info("Reading file locally - path: {}", storagePath);
        try {
            return Files.readAllBytes(validateAndResolvePath(storagePath));
        } catch (IOException e) {
            log.error("Failed to read file locally - path: {}", storagePath, e);
            throw new LocalStorageException("Failed to read file: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String storagePath) {
        try {
            return Files.exists(validateAndResolvePath(storagePath));
        } catch (LocalStorageException e) {
            // If path is outside base directory, it doesn't exist in our storage
            return false;
        }
    }

    @Override
    public void delete(String storagePath) {
        log.info("Deleting file locally - path: {}", storagePath);
        try {
            Files.deleteIfExists(validateAndResolvePath(storagePath));
        } catch (IOException e) {
            log.error("Failed to delete file locally - path: {}", storagePath, e);
            throw new LocalStorageException("Failed to delete file: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateAccessUrl(String storagePath, java.time.Duration ttl) {
        // 로컬 스토리지는 TTL 기반 URL을 지원하지 않으므로 경로를 그대로 반환
        return storagePath;
    }

    @Override
    public String generateChecksum(String content) {
        return ChecksumUtils.generateSha256(content);
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.LOCAL;
    }
}

