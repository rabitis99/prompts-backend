package org.example.sharedprompts.module.domain.production.service.production.presign;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.config.condition.ConditionalOnStorageType;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@ConditionalOnStorageType("S3")
public class S3PresignedUrlGenerator implements PresignedUrlGenerator {

    private final S3Presigner s3Presigner;

    @Override
    public String generate(String bucket, String key, Duration ttl) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("Bucket name cannot be null or blank");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("S3 key cannot be null or blank");
        }
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("TTL must be a positive duration");
        }
        
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(b -> b
                        .bucket(bucket)
                        .key(key))
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(request);
        return presigned.url().toString();
    }
}
