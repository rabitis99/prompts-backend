package org.example.sharedprompts.module.domain.production.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.util.ChecksumUtils;
import org.example.sharedprompts.global.util.ContentTypeUtils;
import org.example.sharedprompts.module.domain.production.service.storage.exception.S3StorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(name = "production.storage.type", havingValue = "S3")
@Slf4j
public class S3StorageStrategy implements StorageStrategy {

    private final S3Client s3Client;

    @Value("${production.storage.s3.bucket}")
    private String bucket;

    @Value("${production.storage.s3.prefix:production}")
    private String prefix;

    public S3StorageStrategy(S3Client s3Client) {
        this.s3Client = s3Client;
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
    public String generateChecksum(String content) {
        return ChecksumUtils.generateSha256(content);
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.S3;
    }

    private String buildS3Key(Long userId, String jobId, String fileName) {
        return String.format("%s/%d/%s/%s", prefix, userId, jobId, fileName);
    }
}
