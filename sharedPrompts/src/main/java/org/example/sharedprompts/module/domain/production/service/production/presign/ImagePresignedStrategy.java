package org.example.sharedprompts.module.domain.production.service.production.presign;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.infra.storage.S3PresignedUrlService;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImagePresignedStrategy implements PresignedStrategy {

    private final S3PresignedUrlService presignedUrlService;

    @Override
    public boolean supports(String contentType) {
        return contentType != null && contentType.toLowerCase().startsWith("image/");
    }

    @Override
    public String generatePresignedUrl(String bucket, String key, String contentType, Duration ttl) {
        log.debug("Generating presigned URL for image - bucket: {}, key: {}, contentType: {}", 
                bucket, key, contentType);
        return presignedUrlService.generatePreviewUrl(bucket, key, ttl);
    }
}

