package org.example.sharedprompts.module.domain.production.service.production;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.service.production.presign.PresignedUrlGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "production.storage.type", havingValue = "S3")
public class ArtifactAccessServiceImpl implements ArtifactAccessService {

    private final PresignedUrlGenerator presignedUrlGenerator;
    private final StringRedisTemplate redisTemplate;

    @Value("${production.storage.s3.bucket}")
    private String bucket;

    @Value("${artifact.url.default-ttl:300}")
    private int cacheTtlSeconds;

    private static final String CACHE_PREFIX = "presigned:";
    private static final Duration PRESIGNED_URL_TTL = Duration.ofMinutes(5);

    @Override
    public String generatePreviewUrl(String filePath) {
        return generate(filePath, "preview");
    }

    @Override
    public String generateDownloadUrl(String filePath) {
        return generate(filePath, "download");
    }

    private String generate(String filePath, String type) {
        String key = extractS3Key(filePath);
        String cacheKey = CACHE_PREFIX + type + ":" + key;

        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Presigned URL cache hit - key: {}", key);
            return cached;
        }

        try {
            String url = presignedUrlGenerator.generate(bucket, key, PRESIGNED_URL_TTL);

            redisTemplate.opsForValue()
                    .set(cacheKey, url, Duration.ofSeconds(cacheTtlSeconds));

            log.debug("Presigned URL generated - key: {}, type: {}", key, type);
            return url;

        } catch (Exception e) {
            log.error("Presigned URL generation failed - key: {}", key, e);
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    private String extractS3Key(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        if (filePath.startsWith("s3://")) {
            String withoutPrefix = filePath.substring(5);
            int slashIndex = withoutPrefix.indexOf('/');
            return slashIndex > 0
                    ? withoutPrefix.substring(slashIndex + 1)
                    : withoutPrefix;
        }

        return filePath;
    }
}
