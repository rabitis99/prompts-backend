package org.example.sharedprompts.module.domain.production.service.production;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.application.storage.StorageFacade;
import org.example.sharedprompts.module.domain.production.service.cdn.CdnUrlProvider;
import org.example.sharedprompts.module.domain.production.util.s3.S3PathUtils;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class ArtifactAccessServiceImpl implements ArtifactAccessService {

    private final StorageFacade storageFacade;
    private final StringRedisTemplate redisTemplate;
    private final CdnUrlProvider cdnUrlProvider;
    private final Duration presignedUrlTtl;
    private final Duration cacheTtl;

    private static final String CACHE_PREFIX = "presigned:";

    public ArtifactAccessServiceImpl(
            StorageFacade storageFacade,
            StringRedisTemplate redisTemplate,
            CdnUrlProvider cdnUrlProvider,
            @Value("${artifact.url.default-ttl:300}") int ttlSeconds) {
        this.storageFacade = storageFacade;
        this.redisTemplate = redisTemplate;
        this.cdnUrlProvider = cdnUrlProvider;
        this.presignedUrlTtl = Duration.ofSeconds(ttlSeconds);
        this.cacheTtl = Duration.ofSeconds(Math.max(ttlSeconds - 30, ttlSeconds / 2));
    }

    @Override
    public String generatePreviewUrl(String filePath) {
        String key = extractS3Key(filePath);
        String cacheKey = CACHE_PREFIX + "preview:" + key;

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
            String url = storageFacade.generatePreviewUrl(key, presignedUrlTtl);

            try {
                redisTemplate.opsForValue()
                        .set(cacheKey, url, cacheTtl);
            } catch (Exception e) {
                log.warn("Redis cache write failed - key: {}", key, e);
            }

            log.debug("Presigned URL generated - key: {}, type: preview", key);
            return url;

        } catch (Exception e) {
            log.error("Presigned URL generation failed - key: {}", key, e);
            throw new BaseException(ModuleErrorCode.STORAGE_ERROR, e);
        }
    }

    @Override
    public String generateDownloadUrl(String filePath) {
        String key = extractS3Key(filePath);
        String cacheKey = CACHE_PREFIX + "download:" + key;

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
            String url = storageFacade.generateDownloadUrl(key, presignedUrlTtl);

            try {
                redisTemplate.opsForValue()
                        .set(cacheKey, url, cacheTtl);
            } catch (Exception e) {
                log.warn("Redis cache write failed - key: {}", key, e);
            }

            log.debug("Presigned URL generated - key: {}, type: download", key);
            return url;

        } catch (Exception e) {
            log.error("Presigned URL generation failed - key: {}", key, e);
            throw new BaseException(ModuleErrorCode.STORAGE_ERROR, e);
        }
    }

    @Override
    public String generateCdnUrl(String filePath) {
        if (!cdnUrlProvider.isEnabled()) {
            return null;
        }
        try {
            String key = extractS3Key(filePath);
            return cdnUrlProvider.generateUrl(key);
        } catch (Exception e) {
            log.warn("CDN URL generation failed - filePath: {}", filePath, e);
            return null;
        }
    }

    private String extractS3Key(String filePath) {
        try {
            return S3PathUtils.extractKey(filePath);
        } catch (IllegalArgumentException e) {
            throw new BaseException(ModuleErrorCode.VALIDATION_ERROR, "filePath", e.getMessage(), e);
        }
    }
}
