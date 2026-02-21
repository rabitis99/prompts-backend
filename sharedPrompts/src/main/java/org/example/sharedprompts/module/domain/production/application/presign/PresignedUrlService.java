package org.example.sharedprompts.module.domain.production.application.presign;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.config.properties.ProductionS3Properties;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.infra.storage.S3PresignedUrlService;
import org.example.sharedprompts.module.domain.production.model.storage.S3Path;
import org.example.sharedprompts.module.domain.production.service.production.presign.PresignedStrategy;
import org.example.sharedprompts.module.domain.production.service.production.presign.PresignedStrategyResolver;
import org.example.sharedprompts.module.domain.production.util.ArtifactMetadataHelper;
import org.example.sharedprompts.module.domain.production.util.ContentDispositionBuilder;
import org.example.sharedprompts.module.domain.production.util.s3.S3PathUtils;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Presigned URL 생성 서비스 (단일 진입점).
 * <p>
 * - S3 경로 파싱/파일명 처리/Content-Disposition 생성 로직을 통합합니다.
 * - Content-Type 기반 전략(Strategy)을 통해 preview/download 정책을 일관되게 적용합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PresignedUrlService {

    private final S3PresignedUrlService s3PresignedUrlService;
    private final PresignedStrategyResolver presignedStrategyResolver;
    private final ProductionS3Properties productionS3Properties;

    public String generateDownloadUrlByKey(String s3PathOrKey, Duration ttl) {
        String key = extractKeyOrThrow(s3PathOrKey, "s3Key");
        String fileName = ArtifactMetadataHelper.extractFileName(key);
        String contentDisposition = ContentDispositionBuilder.attachment(fileName);
        return s3PresignedUrlService.generateDownloadUrl(key, ttl, contentDisposition);
    }

    public String generatePreviewUrlByKey(String s3PathOrKey, Duration ttl) {
        String key = extractKeyOrThrow(s3PathOrKey, "s3Key");
        return s3PresignedUrlService.generatePreviewUrl(key, ttl);
    }

    public String generateUploadUrlByKey(String s3Key, String contentType, Duration ttl) {
        String key = extractKeyOrThrow(s3Key, "s3Key");
        return s3PresignedUrlService.generateUploadUrl(key, contentType, ttl);
    }

    /**
     * 아티팩트(Content-Type + S3 경로) 기반 Presigned URL 생성.
     * 기존 동작과 동일하게, 파싱 실패/설정 누락 등은 null을 반환하여 응답에서 presignedUrl이 비어있을 수 있습니다.
     */
    public String generateForArtifact(ProductionArtifactDetailEntity artifact, Duration ttl) {
        if (artifact == null) {
            throw new BaseException(ModuleErrorCode.VALIDATION_ERROR, "artifact", "artifact must not be null");
        }

        if (artifact.getS3Key() == null || artifact.getS3Key().isBlank()) {
            return null; // TEXT 타입 등
        }

        String contentType = artifact.getContentType();
        if (contentType == null || contentType.isBlank()) {
            log.warn("ContentType is not set for artifact - artifactId: {}", artifact.getId());
            contentType = "application/octet-stream";
        }

        PresignedStrategy strategy = presignedStrategyResolver.resolve(contentType);

        S3Path location = S3PathUtils.tryParse(artifact.getS3Key(), productionS3Properties.getBucket()).orElse(null);
        if (location == null
                || location.bucket() == null || location.bucket().isBlank()
                || location.key() == null || location.key().isBlank()) {
            log.warn("Failed to parse S3 path for artifact - artifactId: {}, s3Key: {}",
                    artifact.getId(), artifact.getS3Key());
            return null;
        }

        return strategy.generatePresignedUrl(location.bucket(), location.key(), contentType, ttl);
    }

    private String extractKeyOrThrow(String s3PathOrKey, String fieldName) {
        try {
            return S3PathUtils.extractKey(s3PathOrKey);
        } catch (IllegalArgumentException e) {
            throw new BaseException(ModuleErrorCode.VALIDATION_ERROR, fieldName, e.getMessage(), e);
        }
    }
}


