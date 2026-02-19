package org.example.sharedprompts.module.domain.production.application.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.infra.storage.S3KeyGenerator;
import org.example.sharedprompts.module.domain.production.infra.storage.S3PresignedUrlService;
import org.example.sharedprompts.module.domain.production.model.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Storage Command Service
 * Presigned URL 생성 등 명령 작업을 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageCommandService {

    private final S3PresignedUrlService presignedUrlService;
    private final S3KeyGenerator keyGenerator;

    @Value("${production.storage.s3.presigned-url.ttl-seconds:300}")
    private int defaultTtlSeconds;

    /**
     * 업로드용 Presigned URL 생성
     */
    public String generateUploadPresignedUrl(Long userId, String jobId, String fileName, String contentType) {
        String tenantId = TenantContext.getCurrentTenantId();
        String s3Key = keyGenerator.generateKey(tenantId, userId, jobId, fileName);
        Duration ttl = Duration.ofSeconds(defaultTtlSeconds);
        return presignedUrlService.generateUploadUrl(s3Key, contentType, ttl);
    }

    /**
     * 다운로드용 Presigned URL 생성
     */
    public String generateDownloadPresignedUrl(String s3Key) {
        Duration ttl = Duration.ofSeconds(defaultTtlSeconds);
        return presignedUrlService.generateDownloadUrl(s3Key, ttl);
    }

    /**
     * 미리보기용 Presigned URL 생성
     */
    public String generatePreviewPresignedUrl(String s3Key) {
        Duration ttl = Duration.ofSeconds(defaultTtlSeconds);
        return presignedUrlService.generatePreviewUrl(s3Key, ttl);
    }
}

