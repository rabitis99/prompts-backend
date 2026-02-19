package org.example.sharedprompts.module.domain.production.service.production.presign;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 이미지 파일용 Presigned URL 생성 전략
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImagePresignedStrategy implements PresignedStrategy {

    private final PresignedUrlGenerator presignedUrlGenerator;

    @Override
    public boolean supports(String contentType) {
        if (contentType == null) {
            return false;
        }
        return contentType.toLowerCase().startsWith("image/");
    }

    @Override
    public String generatePresignedUrl(String bucket, String key, String contentType, Duration ttl) {
        log.debug("Generating presigned URL for image - bucket: {}, key: {}, contentType: {}", 
                bucket, key, contentType);
        return presignedUrlGenerator.generate(bucket, key, ttl);
    }
}

