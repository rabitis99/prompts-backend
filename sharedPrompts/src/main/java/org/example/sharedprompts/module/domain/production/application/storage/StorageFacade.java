package org.example.sharedprompts.module.domain.production.application.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.application.presign.PresignedUrlService;
import org.example.sharedprompts.module.domain.production.infra.storage.S3DownloadService;
import org.example.sharedprompts.module.domain.production.infra.storage.S3UploadService;
import org.example.sharedprompts.module.domain.production.model.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

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
    private final PresignedUrlService presignedUrlService;

    /**
     * 파일 업로드
     * TenantContext에서 tenantId를 가져옵니다.
     * 비동기 컨텍스트 등에서 tenantId가 null일 수 있습니다.
     */
    public String upload(String content, Long userId, String jobId, String fileName) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            log.warn("TenantContext.getCurrentTenantId() returned null - userId: {}, jobId: {}, fileName: {}. " +
                    "File will be stored without tenant prefix. Consider using tenant-aware upload method in async contexts.",
                    userId, jobId, fileName);
        }
        return uploadService.upload(content, tenantId, userId, jobId, fileName);
    }

    /**
     * 바이너리 파일 업로드
     * TenantContext에서 tenantId를 가져옵니다.
     * 비동기 컨텍스트 등에서 tenantId가 null일 수 있습니다.
     */
    public String upload(byte[] data, String contentType, Long userId, String jobId, String fileName) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            log.warn("TenantContext.getCurrentTenantId() returned null - userId: {}, jobId: {}, fileName: {}. " +
                    "File will be stored without tenant prefix. Consider using tenant-aware upload method in async contexts.",
                    userId, jobId, fileName);
        }
        return uploadService.upload(data, contentType, tenantId, userId, jobId, fileName);
    }

    /**
     * 테넌트 인식 파일 업로드
     * 명시적으로 전달된 tenantId를 사용합니다.
     * 비동기 컨텍스트 등에서 TenantContext가 사용 불가능한 경우 사용합니다.
     */
    public String upload(String content, String tenantId, Long userId, String jobId, String fileName) {
        return uploadService.upload(content, tenantId, userId, jobId, fileName);
    }

    /**
     * 테넌트 인식 바이너리 파일 업로드
     * 명시적으로 전달된 tenantId를 사용합니다.
     * 비동기 컨텍스트 등에서 TenantContext가 사용 불가능한 경우 사용합니다.
     */
    public String upload(byte[] data, String contentType, String tenantId, Long userId, String jobId, String fileName) {
        return uploadService.upload(data, contentType, tenantId, userId, jobId, fileName);
    }

    /**
     * 파일 다운로드
     */
    public byte[] download(String s3Key) {
        return downloadService.download(s3Key);
    }

    /**
     * 파일의 일부만 다운로드 (Range 요청)
     * 이미지 포맷 감지 등 파일의 시작 부분만 필요한 경우에 사용합니다.
     *
     * @param s3Key S3 객체 키
     * @param startByteRange 시작 바이트 위치 (0-based, inclusive)
     * @param endByteRange 종료 바이트 위치 (inclusive)
     * @return 파일의 일부 내용 (byte 배열)
     */
    public byte[] downloadRange(String s3Key, long startByteRange, long endByteRange) {
        return downloadService.downloadRange(s3Key, startByteRange, endByteRange);
    }

    /**
     * S3 객체의 Content-Length(바이트)를 반환합니다.
     * 객체가 없거나 메타데이터 조회에 실패하면 빈 Optional을 반환합니다.
     */
    public Optional<Long> getContentLength(String s3Key) {
        return downloadService.getContentLength(s3Key);
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
        uploadService.delete(s3Key);
    }

    /**
     * 다운로드용 Presigned URL 생성
     */
    public String generateDownloadUrl(String s3Key, Duration ttl) {
        return presignedUrlService.generateDownloadUrlByKey(s3Key, ttl);
    }

    /**
     * 이미지 미리보기용 Presigned URL 생성
     */
    public String generatePreviewUrl(String s3Key, Duration ttl) {
        return presignedUrlService.generatePreviewUrlByKey(s3Key, ttl);
    }

    /**
     * 업로드용 Presigned URL 생성
     */
    public String generateUploadUrl(String s3Key, String contentType, Duration ttl) {
        return presignedUrlService.generateUploadUrlByKey(s3Key, contentType, ttl);
    }

}

