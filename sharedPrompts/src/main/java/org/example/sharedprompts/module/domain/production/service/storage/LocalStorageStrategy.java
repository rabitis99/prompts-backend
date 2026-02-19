package org.example.sharedprompts.module.domain.production.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.util.ChecksumUtils;
import org.example.sharedprompts.module.domain.production.service.storage.exception.LocalStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@ConditionalOnProperty(name = "production.storage.type", havingValue = "LOCAL", matchIfMissing = true)
@Slf4j
public class LocalStorageStrategy implements StorageStrategy {

    @Value("${production.storage.local.base-path:./storage/production}")
    private String basePath;

    @Override
    public String store(String content, Long userId, String jobId, String fileName) {
        return store(content, null, userId, jobId, fileName);
    }

    @Override
    public String store(byte[] data, String contentType, Long userId, String jobId, String fileName) {
        return store(data, contentType, null, userId, jobId, fileName);
    }

    @Override
    public String store(String content, String tenantId, Long userId, String jobId, String fileName) {
        log.info("Storing file locally - tenantId: {}, userId: {}, jobId: {}, fileName: {}",
                tenantId, userId, jobId, fileName);
        try {
            Path directory = buildDirectory(tenantId, userId, jobId);
            Path filePath = directory.resolve(fileName);
            validateWithinBasePath(filePath);
            Files.createDirectories(directory);
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
            log.info("File stored successfully - path: {}", filePath);
            return filePath.toString();
        } catch (IOException e) {
            log.error("Failed to store file locally - tenantId: {}, userId: {}", tenantId, userId, e);
            throw new LocalStorageException("Failed to store file: " + e.getMessage(), e);
        }
    }

    @Override
    public String store(byte[] data, String contentType, String tenantId, Long userId, String jobId, String fileName) {
        log.info("Storing binary file locally - tenantId: {}, userId: {}, jobId: {}, fileName: {}",
                tenantId, userId, jobId, fileName);
        try {
            Path directory = buildDirectory(tenantId, userId, jobId);
            Path filePath = directory.resolve(fileName);
            validateWithinBasePath(filePath);
            Files.createDirectories(directory);
            Files.write(filePath, data);
            log.info("Binary file stored successfully - path: {}, size: {} bytes", filePath, data.length);
            return filePath.toString();
        } catch (IOException e) {
            log.error("Failed to store binary file locally - tenantId: {}, userId: {}", tenantId, userId, e);
            throw new LocalStorageException("Failed to store binary file: " + e.getMessage(), e);
        }
    }

    /**
     * 경로 세그먼트에 경로 탐색 시도가 있는지 검증합니다.
     * 
     * @param value 검증할 값
     * @param paramName 파라미터 이름 (에러 메시지용)
     * @throws LocalStorageException 경로 탐색 시도가 감지된 경우
     */
    private void validatePathSegment(String value, String paramName) {
        if (value != null && (value.contains("..") || value.contains("/") || value.contains("\\"))) {
            throw new LocalStorageException("Invalid " + paramName + ": path traversal detected");
        }
    }

    /**
     * 테넌트 인식 디렉토리 경로를 생성합니다.
     * 
     * @param tenantId 테넌트 ID (null 가능)
     * @param userId 사용자 ID
     * @param jobId 작업 ID
     * @return 생성된 디렉토리 Path
     */
    private Path buildDirectory(String tenantId, Long userId, String jobId) {
        validatePathSegment(jobId, "jobId");
        if (tenantId != null && !tenantId.isBlank()) {
            validatePathSegment(tenantId, "tenantId");
            return Paths.get(basePath, tenantId, String.valueOf(userId), jobId);
        }
        return Paths.get(basePath, String.valueOf(userId), jobId);
    }

    /**
     * 파일 경로가 basePath 내에 있는지 검증합니다.
     * 
     * @param filePath 검증할 파일 경로
     * @throws LocalStorageException basePath 밖의 경로인 경우
     */
    private void validateWithinBasePath(Path filePath) {
        if (!filePath.normalize().toAbsolutePath().startsWith(
                Paths.get(basePath).normalize().toAbsolutePath())) {
            throw new LocalStorageException("Invalid fileName: path traversal detected");
        }
    }

    /**
     * 저장 경로를 검증하고 절대 경로로 변환합니다.
     * basePath 밖의 경로는 접근을 거부합니다.
     * 
     * @param storagePath 검증할 저장 경로
     * @return 검증된 절대 경로
     * @throws LocalStorageException basePath 밖의 경로인 경우
     */
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
        // 로컬 스토리지는 개발 환경 전용 - 운영 환경에서는 S3 등 외부 스토리지 사용 필요
        log.warn("LocalStorage does not support secure access URLs. " +
                "Internal path returned - do not use in production.");
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

