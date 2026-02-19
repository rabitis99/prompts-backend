package org.example.sharedprompts.module.domain.production.application.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.infra.storage.S3DownloadService;
import org.example.sharedprompts.module.domain.production.infra.storage.S3PresignedUrlService;
import org.example.sharedprompts.module.domain.production.infra.storage.S3UploadService;
import org.example.sharedprompts.module.domain.production.model.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Storage Facade
 * 비즈니스 로직과 인프라 로직을 분리하는 퍼사드 패턴 구현
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageFacade {

    private final S3UploadService uploadService;
    private final S3DownloadService downloadService;
    private final S3PresignedUrlService presignedUrlService;

    /**
     * 파일 업로드
     */
    public String upload(String content, Long userId, String jobId, String fileName) {
        String tenantId = TenantContext.getCurrentTenantId();
        return uploadService.upload(content, tenantId, userId, jobId, fileName);
    }

    /**
     * 바이너리 파일 업로드
     */
    public String upload(byte[] data, String contentType, Long userId, String jobId, String fileName) {
        String tenantId = TenantContext.getCurrentTenantId();
        return uploadService.upload(data, contentType, tenantId, userId, jobId, fileName);
    }

    /**
     * 파일 다운로드
     */
    public byte[] download(String s3Key) {
        return downloadService.download(s3Key);
    }

    /**
     * 파일 존재 여부 확인
     */
    public boolean exists(String s3Key) {
        return downloadService.exists(s3Key);
    }

    /**
     * 파일 삭제
     */
    public void delete(String s3Key) {
        downloadService.delete(s3Key);
    }

    /**
     * 다운로드용 Presigned URL 생성
     */
    public String generateDownloadUrl(String s3Key, Duration ttl) {
        String fileName = extractFileName(s3Key);
        String contentDisposition = fileName != null 
                ? String.format("attachment; filename=\"%s\"", fileName)
                : "attachment";
        return presignedUrlService.generateDownloadUrl(s3Key, ttl, contentDisposition);
    }

    /**
     * 이미지 미리보기용 Presigned URL 생성
     */
    public String generatePreviewUrl(String s3Key, Duration ttl) {
        return presignedUrlService.generatePreviewUrl(s3Key, ttl);
    }

    /**
     * 업로드용 Presigned URL 생성
     */
    public String generateUploadUrl(String s3Key, String contentType, Duration ttl) {
        return presignedUrlService.generateUploadUrl(s3Key, contentType, ttl);
    }

    private String extractFileName(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            return null;
        }
        int lastSlash = s3Key.lastIndexOf('/');
        return lastSlash >= 0 && lastSlash < s3Key.length() - 1 
                ? s3Key.substring(lastSlash + 1) 
                : s3Key;
    }
}

