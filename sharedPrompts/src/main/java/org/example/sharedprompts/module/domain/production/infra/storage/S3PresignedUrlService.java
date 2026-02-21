package org.example.sharedprompts.module.domain.production.infra.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.config.condition.ConditionalOnStorageType;
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

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * S3 Presigned URL 서비스
 * 업로드/다운로드용 Presigned URL 생성을 담당합니다.
 */
@Service
@ConditionalOnStorageType("S3")
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

    private void validateBucketAndKey(String bucket, String s3Key) {
        if (bucket == null || bucket.isBlank()) {
            throw new S3StorageException("S3 bucket is not configured");
        }
        if (s3Key == null || s3Key.isBlank()) {
            throw new S3StorageException("S3 key is blank");
        }
    }

    /**
     * Presigned URL 생성 시 서버 시간 진단 로그 출력.
     * "Request has expired" 발생 시 S3 응답의 ServerTime과 X-Amz-Date를 비교해 클록 드리프트 여부를 확인하세요.
     */
    private void logPresignTimeDiagnostics(String presignedUrl, String operation) {
        Instant now = Instant.now();
        ZonedDateTime utcNow = ZonedDateTime.now(ZoneOffset.UTC);
        ZoneId systemZone = ZoneId.systemDefault();
        String xAmzDate = extractXAmzDateFromPresignedUrl(presignedUrl);
        log.debug("[Presign time diagnostic] operation={} | Instant.now()={} | ZonedDateTime.now(UTC)={} | systemDefaultZone={} | X-Amz-Date(in URL)={}",
                operation, now, utcNow, systemZone, xAmzDate != null ? xAmzDate : "N/A");
    }

    /**
     * Presigned URL 쿼리 스트링에서 X-Amz-Date 값을 추출합니다.
     * 형식: yyyyMMddTHHmmssZ (ISO 8601, UTC).
     */
    private String extractXAmzDateFromPresignedUrl(String presignedUrl) {
        if (presignedUrl == null || presignedUrl.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(presignedUrl);
            String query = uri.getQuery();
            if (query == null) {
                return null;
            }
            for (String param : query.split("&")) {
                int eq = param.indexOf('=');
                if (eq > 0 && "X-Amz-Date".equals(param.substring(0, eq))) {
                    return param.substring(eq + 1);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract X-Amz-Date from presigned URL: {}", e.getMessage());
        }
        return null;
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
        validateBucketAndKey(bucket, s3Key);
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
            String url = presigned.url().toString();
            logPresignTimeDiagnostics(url, "download");
            return url;
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
        validateBucketAndKey(bucket, s3Key);
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
            String url = presigned.url().toString();
            logPresignTimeDiagnostics(url, "preview");
            return url;
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
        validateBucketAndKey(bucket, s3Key);
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
            String url = presigned.url().toString();
            logPresignTimeDiagnostics(url, "upload");
            return url;
        } catch (Exception e) {
            log.error("S3 presigned upload URL generation failed - key: {}", s3Key, e);
            throw new S3StorageException("Presigned upload URL generation failed: " + e.getMessage(), e);
        }
    }
}

