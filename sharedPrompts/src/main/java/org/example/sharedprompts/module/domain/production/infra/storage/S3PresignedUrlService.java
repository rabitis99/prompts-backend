package org.example.sharedprompts.module.domain.production.infra.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.config.properties.ProductionS3Properties;
import org.example.sharedprompts.module.domain.production.infra.storage.exception.S3StorageException;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Duration;

/**
 * S3 Presigned URL 서비스
 * 업로드/다운로드용 Presigned URL 생성을 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3PresignedUrlService {

    private final S3Presigner s3Presigner;
    private final ProductionS3Properties productionS3Properties;

    private String bucket() {
        return productionS3Properties.getBucket();
    }

    private int defaultTtlSeconds() {
        return productionS3Properties.getPresignedUrl().getTtlSeconds();
    }

    /**
     * 다운로드용 Presigned GET URL을 생성합니다.
     */
    public String generateDownloadUrl(String s3Key, Duration ttl) {
        return generateDownloadUrl(bucket(), s3Key, ttl, null);
    }

    /**
     * 다운로드용 Presigned GET URL을 생성합니다 (Content-Disposition 포함).
     */
    public String generateDownloadUrl(String s3Key, Duration ttl, String contentDisposition) {
        return generateDownloadUrl(bucket(), s3Key, ttl, contentDisposition);
    }

    /**
     * 다운로드용 Presigned GET URL을 생성합니다 (버킷 지정).
     * <p>
     * - 기존 코드의 "bucket을 외부에서 전달하는 presign 구현"을 단일 구현으로 흡수하기 위해 추가합니다.
     * - 기존 시그니처는 변경하지 않고, 내부에서 이 메서드를 호출합니다.
     */
    public String generateDownloadUrl(String bucket, String s3Key, Duration ttl, String contentDisposition) {
        log.debug("Generating presigned download URL - bucket: {}, key: {}, ttl: {}", bucket, s3Key, ttl);
        if (bucket == null || bucket.isBlank()) {
            throw new S3StorageException("S3 bucket is not configured");
        }
        if (s3Key == null || s3Key.isBlank()) {
            throw new S3StorageException("S3 key is blank");
        }
        try {
            GetObjectRequest.Builder requestBuilder = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key);
            
            if (contentDisposition != null && !contentDisposition.isBlank()) {
                requestBuilder.responseContentDisposition(contentDisposition);
            }
            
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(ttl != null ? ttl : Duration.ofSeconds(defaultTtlSeconds()))
                    .getObjectRequest(requestBuilder.build())
                    .build();
            
            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
            return presigned.url().toString();
        } catch (Exception e) {
            log.error("S3 presigned download URL generation failed - key: {}", s3Key, e);
            throw new S3StorageException("Presigned URL generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * 이미지 미리보기용 Presigned GET URL을 생성합니다 (inline).
     */
    public String generatePreviewUrl(String s3Key, Duration ttl) {
        return generatePreviewUrl(bucket(), s3Key, ttl);
    }

    /**
     * 이미지 미리보기용 Presigned GET URL을 생성합니다 (inline, 버킷 지정).
     */
    public String generatePreviewUrl(String bucket, String s3Key, Duration ttl) {
        log.debug("Generating presigned preview URL - bucket: {}, key: {}, ttl: {}", bucket, s3Key, ttl);
        if (bucket == null || bucket.isBlank()) {
            throw new S3StorageException("S3 bucket is not configured");
        }
        if (s3Key == null || s3Key.isBlank()) {
            throw new S3StorageException("S3 key is blank");
        }
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .responseContentDisposition("inline")
                    .build();
            
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(ttl != null ? ttl : Duration.ofSeconds(defaultTtlSeconds()))
                    .getObjectRequest(request)
                    .build();
            
            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
            return presigned.url().toString();
        } catch (Exception e) {
            log.error("S3 presigned preview URL generation failed - key: {}", s3Key, e);
            throw new S3StorageException("Presigned URL generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * 업로드용 Presigned PUT URL을 생성합니다.
     */
    public String generateUploadUrl(String s3Key, String contentType, Duration ttl) {
        return generateUploadUrl(bucket(), s3Key, contentType, ttl);
    }

    /**
     * 업로드용 Presigned PUT URL을 생성합니다 (버킷 지정).
     */
    public String generateUploadUrl(String bucket, String s3Key, String contentType, Duration ttl) {
        log.debug("Generating presigned upload URL - bucket: {}, key: {}, contentType: {}, ttl: {}",
                bucket, s3Key, contentType, ttl);
        if (bucket == null || bucket.isBlank()) {
            throw new S3StorageException("S3 bucket is not configured");
        }
        if (s3Key == null || s3Key.isBlank()) {
            throw new S3StorageException("S3 key is blank");
        }
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentType(contentType)
                    .build();
            
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(ttl != null ? ttl : Duration.ofSeconds(defaultTtlSeconds()))
                    .putObjectRequest(request)
                    .build();
            
            PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
            return presigned.url().toString();
        } catch (Exception e) {
            log.error("S3 presigned upload URL generation failed - key: {}", s3Key, e);
            throw new S3StorageException("Presigned upload URL generation failed: " + e.getMessage(), e);
        }
    }
}

