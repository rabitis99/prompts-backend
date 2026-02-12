package org.example.sharedprompts.module.domain.production.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.util.ChecksumUtils;
import org.example.sharedprompts.global.util.ContentTypeUtils;
import org.example.sharedprompts.module.domain.production.service.storage.exception.S3StorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@ConditionalOnProperty(name = "production.storage.type", havingValue = "S3")
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
    }

    @Override
    public String store(String content, Long userId, String jobId, String fileName) {
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        String contentType = ContentTypeUtils.guessContentType(fileName);
        return store(data, contentType, userId, jobId, fileName);
    }

    @Override
    public String store(byte[] data, String contentType, Long userId, String jobId, String fileName) {
        String s3Key = buildS3Key(userId, jobId, fileName);

        log.info("Uploading to S3 - bucket: {}, key: {}, contentType: {}, size: {} bytes",
                bucket, s3Key, contentType, data.length);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentType(contentType)
                    .contentLength((long) data.length)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(data));

            String s3Url = String.format("s3://%s/%s", bucket, s3Key);
            log.info("S3 upload completed - url: {}", s3Url);

            return s3Key;

        } catch (Exception e) {
            log.error("S3 upload failed - bucket: {}, key: {}", bucket, s3Key, e);
            throw new S3StorageException("S3 upload failed: " + e.getMessage(), e);
        }
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
        String s3Key = buildS3Key(tenantId, userId, jobId, fileName);

        log.info("Uploading to S3 - bucket: {}, key: {}, contentType: {}, size: {} bytes",
                bucket, s3Key, contentType, data.length);

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

    private String buildS3Key(Long userId, String jobId, String fileName) {
        return String.format("%s/%d/%s/%s", prefix, userId, jobId, fileName);
    }

    private String buildS3Key(String tenantId, Long userId, String jobId, String fileName) {
        if (tenantId == null || tenantId.isBlank()) {
            return buildS3Key(userId, jobId, fileName);
        }
        return String.format("%s/%s/%d/%s/%s", prefix, tenantId, userId, jobId, fileName);
    }
}
