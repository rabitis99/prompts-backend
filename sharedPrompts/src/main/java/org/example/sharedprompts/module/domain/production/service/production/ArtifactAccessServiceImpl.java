package org.example.sharedprompts.module.domain.production.service.production;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.service.production.presign.PresignedUrlGenerator;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@ConditionalOnProperty(name = "production.storage.type", havingValue = "S3")
public class ArtifactAccessServiceImpl implements ArtifactAccessService {

    private final PresignedUrlGenerator presignedUrlGenerator;
    private final StringRedisTemplate redisTemplate;
    private final String bucket;
    private final Duration presignedUrlTtl;
    private final Duration cacheTtl;

    private static final String CACHE_PREFIX = "presigned:";

    public ArtifactAccessServiceImpl(
            PresignedUrlGenerator presignedUrlGenerator,
            StringRedisTemplate redisTemplate,
            @Value("${production.storage.s3.bucket}") String bucket,
            @Value("${artifact.url.default-ttl:300}") int ttlSeconds) {
        this.presignedUrlGenerator = presignedUrlGenerator;
        this.redisTemplate = redisTemplate;
        this.bucket = bucket;
        this.presignedUrlTtl = Duration.ofSeconds(ttlSeconds);
        this.cacheTtl = Duration.ofSeconds(Math.max(ttlSeconds - 30, ttlSeconds / 2));
    }

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

        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("Presigned URL cache hit - key: {}", key);
                return cached;
            }
        } catch (Exception e) {
            log.warn("Redis cache read failed, generating new URL - key: {}", key, e);
        }

        try {
            String url = presignedUrlGenerator.generate(bucket, key, presignedUrlTtl);

            try {
                redisTemplate.opsForValue()
                        .set(cacheKey, url, cacheTtl);
            } catch (Exception e) {
                log.warn("Redis cache write failed - key: {}", key, e);
            }

            log.debug("Presigned URL generated - key: {}, type: {}", key, type);
            return url;

        } catch (Exception e) {
            log.error("Presigned URL generation failed - key: {}", key, e);
            throw new BaseException(ModuleErrorCode.STORAGE_ERROR, e);
        }
    }

    private String extractS3Key(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new BaseException(ModuleErrorCode.VALIDATION_ERROR, "filePath", "File path cannot be null or empty");
        }

        if (filePath.startsWith("s3://")) {
            String withoutPrefix = filePath.substring(5);
            int slashIndex = withoutPrefix.indexOf('/');
            if (slashIndex <= 0) {
                throw new BaseException(ModuleErrorCode.VALIDATION_ERROR, "filePath", "Invalid S3 path - missing object key: " + filePath);
            }
            String objectKey = withoutPrefix.substring(slashIndex + 1);
            if (objectKey.isBlank()) {
                throw new BaseException(ModuleErrorCode.VALIDATION_ERROR, "filePath", "Invalid S3 path - missing object key: " + filePath);
            }
            return objectKey;
        }

        return filePath;
    }
}
