package org.example.sharedprompts.module.domain.production.service.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.util.ChecksumUtils;
import org.example.sharedprompts.module.domain.production.application.storage.StorageFacade;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;

/**
 * S3StorageStrategy
 * StorageStrategy 인터페이스를 구현하여 기존 코드와의 호환성을 유지합니다.
 * 내부적으로는 StorageFacade를 사용합니다.
 */
@Component("s3StorageStrategy")
@RequiredArgsConstructor
@Slf4j
public class S3StorageStrategy implements StorageStrategy {

    private final StorageFacade storageFacade;

    @PostConstruct
    public void initialize() {
        log.info("S3StorageStrategy initialized - storageType: {}", getStorageType());
    }

    @Override
    public String store(String content, Long userId, String jobId, String fileName) {
        return storageFacade.upload(content, userId, jobId, fileName);
    }

    @Override
    public String store(byte[] data, String contentType, Long userId, String jobId, String fileName) {
        return storageFacade.upload(data, contentType, userId, jobId, fileName);
    }

    @Override
    public String store(String content, String tenantId, Long userId, String jobId, String fileName) {
        return storageFacade.upload(content, tenantId, userId, jobId, fileName);
    }

    @Override
    public String store(byte[] data, String contentType, String tenantId, Long userId, String jobId, String fileName) {
        return storageFacade.upload(data, contentType, tenantId, userId, jobId, fileName);
    }

    @Override
    public byte[] read(String storagePath) {
        return storageFacade.download(storagePath);
    }

    @Override
    public boolean exists(String storagePath) {
        return storageFacade.exists(storagePath);
    }

    @Override
    public void delete(String storagePath) {
        storageFacade.delete(storagePath);
    }

    @Override
    public String generateAccessUrl(String storagePath, Duration ttl) {
        return storageFacade.generateDownloadUrl(storagePath, ttl);
    }

    @Override
    public String generateChecksum(String content) {
        return ChecksumUtils.generateSha256(content);
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.S3;
    }
}
