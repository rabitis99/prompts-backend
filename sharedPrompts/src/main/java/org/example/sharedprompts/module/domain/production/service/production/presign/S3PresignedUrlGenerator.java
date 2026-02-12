package org.example.sharedprompts.module.domain.production.service.production.presign;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "production.storage.type", havingValue = "S3")
public class S3PresignedUrlGenerator implements PresignedUrlGenerator {

    private final S3Presigner s3Presigner;

    @Override
    public String generate(String bucket, String key, Duration ttl) {
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
