package org.example.sharedprompts.module.domain.production.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.util.ChecksumUtils;
import org.example.sharedprompts.global.util.ContentTypeUtils;
import org.example.sharedprompts.module.domain.production.config.condition.ConditionalOnStorageType;
import org.example.sharedprompts.module.domain.production.service.storage.exception.S3StorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@ConditionalOnStorageType("S3")
@Slf4j
public class S3StorageStrategy implements StorageStrategy {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${production.storage.s3.bucket}")
    private String bucket;

    @Value("${production.storage.s3.prefix:production}")
    private String prefix;

    public S3StorageStrategy(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        log.info("S3StorageStrategy bean created - S3Client and S3Presigner injected");
    }

    @PostConstruct
    public void initialize() {
        log.info("S3StorageStrategy initialized - bucket: {}, prefix: {}, storageType: {}", 
                bucket, prefix, getStorageType());
    }

    @Override
    public String store(String content, Long userId, String jobId, String fileName) {
        return store(content, null, userId, jobId, fileName);
    }

    @Override
    public String store(byte[] data, String contentType, Long userId, String jobId, String fileName) {
        return store(data, contentType, null, userId, jobId, fileName);
    }

    @Override
    public byte[] read(String storagePath) {
        log.debug("Reading from S3 - bucket: {}, key: {}", bucket, storagePath);
        try {
            ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(storagePath)
                            .build());
            return responseBytes.asByteArray();
        } catch (Exception e) {
            log.error("S3 read failed - bucket: {}, key: {}", bucket, storagePath, e);
            throw new S3StorageException("S3 read failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String storagePath) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(storagePath)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            log.error("S3 exists check failed - bucket: {}, key: {}", bucket, storagePath, e);
            throw new S3StorageException("S3 exists check failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("S3 exists check failed - bucket: {}, key: {}", bucket, storagePath, e);
            throw new S3StorageException("S3 exists check failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String storagePath) {
        log.info("Deleting from S3 - bucket: {}, key: {}", bucket, storagePath);
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(storagePath)
                    .build());
        } catch (Exception e) {
            log.error("S3 delete failed - bucket: {}, key: {}", bucket, storagePath, e);
            throw new S3StorageException("S3 delete failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateAccessUrl(String storagePath, Duration ttl) {
        log.debug("Generating presigned URL - bucket: {}, key: {}, ttl: {}", bucket, storagePath, ttl);
        try {
            GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                    .signatureDuration(ttl)
                    .getObjectRequest(b -> b.bucket(bucket).key(storagePath))
                    .build();
            return s3Presigner.presignGetObject(request).url().toString();
        } catch (Exception e) {
            log.error("S3 presigned URL generation failed - key: {}", storagePath, e);
            throw new S3StorageException("Presigned URL generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateChecksum(String content) {
        return ChecksumUtils.generateSha256(content);
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.S3;
    }

    @Override
    public String store(String content, String tenantId, Long userId, String jobId, String fileName) {
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        String contentType = ContentTypeUtils.guessContentType(fileName);
        return store(data, contentType, tenantId, userId, jobId, fileName);
    }

    @Override
    public String store(byte[] data, String contentType, String tenantId, Long userId, String jobId, String fileName) {
        // contentType 기반으로 올바른 확장자를 가진 파일명 생성
        String correctedFileName = ensureCorrectExtension(fileName, contentType);
        String s3Key = buildS3Key(tenantId, userId, jobId, correctedFileName);

        log.info("Uploading to S3 - bucket: {}, key: {}, contentType: {}, size: {} bytes, originalFileName: {}, correctedFileName: {}",
                bucket, s3Key, contentType, data.length, fileName, correctedFileName);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentType(contentType)
                    .contentLength((long) data.length)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(data));
            log.info("S3 upload completed - key: {}", s3Key);
            return s3Key;

        } catch (Exception e) {
            log.error("S3 upload failed - bucket: {}, key: {}", bucket, s3Key, e);
            throw new S3StorageException("S3 upload failed: " + e.getMessage(), e);
        }
    }

    /**
     * contentType을 기반으로 올바른 확장자를 가진 파일명을 생성합니다.
     * 파일명의 확장자가 contentType과 일치하지 않으면 올바른 확장자로 변경합니다.
     */
    private String ensureCorrectExtension(String fileName, String contentType) {
        if (fileName == null || fileName.isBlank()) {
            fileName = "output";
        }

        // contentType에서 확장자 결정
        String correctExtension = getExtensionFromContentType(contentType);
        
        // 현재 파일명에서 확장자 추출
        int lastDot = fileName.lastIndexOf('.');
        String nameWithoutExt = lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
        String currentExt = lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";

        // 확장자가 올바르지 않거나 없으면 올바른 확장자로 변경
        if (correctExtension != null && !currentExt.equals(correctExtension)) {
            return nameWithoutExt + "." + correctExtension;
        }

        // 확장자가 이미 올바르면 그대로 반환
        return fileName;
    }

    /**
     * contentType에서 적절한 파일 확장자를 반환합니다.
     */
    private String getExtensionFromContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }

        String lowerContentType = contentType.toLowerCase();
        
        // 이미지 타입
        if (lowerContentType.startsWith("image/")) {
            return switch (lowerContentType) {
                case "image/png" -> "png";
                case "image/jpeg", "image/jpg" -> "jpg";
                case "image/gif" -> "gif";
                case "image/webp" -> "webp";
                case "image/bmp" -> "bmp";
                case "image/svg+xml" -> "svg";
                default -> "jpg"; // 기본값
            };
        }
        
        // 문서 타입
        if (lowerContentType.equals("application/pdf")) {
            return "pdf";
        }
        if (lowerContentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            return "xlsx";
        }
        if (lowerContentType.equals("application/vnd.ms-excel")) {
            return "xls";
        }
        if (lowerContentType.equals("text/csv")) {
            return "csv";
        }
        if (lowerContentType.equals("text/html")) {
            return "html";
        }
        if (lowerContentType.equals("text/markdown")) {
            return "md";
        }
        if (lowerContentType.equals("application/json")) {
            return "json";
        }
        if (lowerContentType.equals("text/plain")) {
            return "txt";
        }

        // 알 수 없는 타입은 null 반환 (기존 확장자 유지)
        return null;
    }

    /**
     * 테넌트가 없는 경우의 S3 키를 생성합니다.
     */
    private String buildS3Key(Long userId, String jobId, String fileName) {
        return String.format("%s/%d/%s/%s", prefix, userId, jobId, fileName);
    }

    /**
     * 테넌트 인식 S3 키를 생성합니다.
     * tenantId가 null이거나 비어있으면 테넌트 없는 키를 생성합니다.
     */
    private String buildS3Key(String tenantId, Long userId, String jobId, String fileName) {
        if (tenantId == null || tenantId.isBlank()) {
            return buildS3Key(userId, jobId, fileName);
        }
        return String.format("%s/%s/%d/%s/%s", prefix, tenantId, userId, jobId, fileName);
    }
}
