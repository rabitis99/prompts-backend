package org.example.sharedprompts.module.domain.production.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactEntity;
import org.example.sharedprompts.module.domain.production.repository.production.ProductionArtifactRepository;
import org.example.sharedprompts.module.domain.production.service.production.ArtifactAccessService;
import org.example.sharedprompts.module.domain.production.service.production.presign.PresignedStrategy;
import org.example.sharedprompts.module.domain.production.service.production.presign.PresignedStrategyResolver;
import org.example.sharedprompts.module.dto.response.production.ArtifactDetailResponseDto;
import org.example.sharedprompts.module.dto.response.production.ArtifactDetailResponseDtoMapper;
import org.example.sharedprompts.module.dto.response.production.ArtifactSummaryDto;
import org.example.sharedprompts.module.dto.response.production.ArtifactSummaryDtoMapper;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArtifactApplicationService {

    private final ProductionArtifactRepository productionArtifactRepository;
    private final PresignedStrategyResolver presignedStrategyResolver;
    private final ArtifactAccessService artifactAccessService;

    @Value("${artifact.url.default-ttl:300}")
    private int presignedUrlTtlSeconds;
    
    @Value("${production.storage.s3.bucket:}")
    private String defaultBucket;

    @Transactional(readOnly = true)
    public List<ArtifactSummaryDto> getArtifacts(Long productionId, Long userId) {
        log.info("Artifacts requested - productionId: {}, userId: {}", productionId, userId);
        
        ProductionArtifactEntity production = productionArtifactRepository
                .findByIdWithArtifacts(productionId)
                .orElseThrow(() -> new BaseException(ModuleErrorCode.PRODUCTION_NOT_FOUND));
        
        if (!production.getUserId().equals(userId)) {
            throw new BaseException(ModuleErrorCode.PRODUCTION_FORBIDDEN);
        }
        
        return ArtifactSummaryDtoMapper.toDtoList(production.getArtifacts());
    }

    @Transactional(readOnly = true)
    public ArtifactDetailResponseDto getArtifact(Long productionId, Long artifactId, Long userId) {
        log.info("Artifact detail requested - productionId: {}, artifactId: {}, userId: {}", 
                productionId, artifactId, userId);
        
        ProductionArtifactEntity production = productionArtifactRepository
                .findByIdWithArtifacts(productionId)
                .orElseThrow(() -> new BaseException(ModuleErrorCode.PRODUCTION_NOT_FOUND));
        
        if (!production.getUserId().equals(userId)) {
            throw new BaseException(ModuleErrorCode.PRODUCTION_FORBIDDEN);
        }
        
        ProductionArtifactDetailEntity artifact = production.getArtifacts().stream()
                .filter(detail -> detail.getId().equals(artifactId))
                .findFirst()
                .orElseThrow(() -> new BaseException(ModuleErrorCode.ARTIFACT_NOT_FOUND));
        
        // Presigned URL 생성
        String presignedUrl = generatePresignedUrl(artifact);
        
        // CDN URL 생성 (TEXT 타입은 s3Key가 null일 수 있음)
        String s3Key = artifact.getS3Key();
        String cdnUrl = (s3Key != null && !s3Key.isBlank())
                ? artifactAccessService.generateCdnUrl(s3Key)
                : null;
        
        return ArtifactDetailResponseDtoMapper.toDto(
                production,
                artifact,
                presignedUrl,
                cdnUrl,
                null // thumbnailUrls는 별도 처리 필요
        );
    }

    private String generatePresignedUrl(ProductionArtifactDetailEntity artifact) {
        // TEXT 타입은 s3Key가 null일 수 있음
        if (artifact.getS3Key() == null || artifact.getS3Key().isBlank()) {
            return null;
        }
        
        // DB에 저장된 contentType 사용 (S3 업로드 시점의 실제 값과 일치)
        String contentType = artifact.getContentType();
        if (contentType == null || contentType.isBlank()) {
            log.warn("ContentType is not set for artifact - artifactId: {}", artifact.getId());
            contentType = "application/octet-stream";
        }
        
        PresignedStrategy strategy = presignedStrategyResolver.resolve(contentType);
        if (strategy == null) {
            log.warn("No PresignedStrategy found for artifact - artifactId: {}, contentType: {}", 
                    artifact.getId(), contentType);
            return null;
        }
        
        // s3Key가 s3:// 형식일 수 있으므로 처리
        S3Location location = parseS3Path(artifact.getS3Key());
        if (location == null) {
            log.warn("Failed to parse S3 path for artifact - artifactId: {}, s3Key: {}",
                    artifact.getId(), artifact.getS3Key());
            return null;
        }

        return strategy.generatePresignedUrl(
                location.bucket(),
                location.key(),
                contentType,
                Duration.ofSeconds(presignedUrlTtlSeconds)
        );
    }

    /**
     * S3 경로 정보를 담는 record
     */
    private record S3Location(String bucket, String key) {}

    /**
     * S3 경로 문자열을 파싱하여 bucket과 key를 추출합니다.
     * 
     * @param s3Key S3 경로 (s3://bucket/key 형식 또는 key만 있는 형식)
     * @return S3Location (bucket, key), 파싱 실패 시 null
     */
    private S3Location parseS3Path(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            return null;
        }

        if (s3Key.startsWith("s3://")) {
            String withoutPrefix = s3Key.substring(5);
            int slashIndex = withoutPrefix.indexOf('/');
            if (slashIndex > 0 && slashIndex < withoutPrefix.length() - 1) {
                String bucket = withoutPrefix.substring(0, slashIndex);
                String key = withoutPrefix.substring(slashIndex + 1);
                if (key.isBlank()) {
                    return null;
                }
                return new S3Location(bucket, key);
            }
            // s3://bucket 또는 s3://bucket/ 형식은 유효하지 않음
            return null;
        }

        // s3:// 접두사가 없는 경우 defaultBucket 사용
        // @Value("${...:}")는 빈 문자열을 기본값으로 주입하므로 null이 될 수 없음
        if (defaultBucket.isBlank()) {
            log.error("Cannot determine S3 bucket - s3Key: {}, defaultBucket is not configured", s3Key);
            return null;
        }
        return new S3Location(defaultBucket, s3Key);
    }
}

