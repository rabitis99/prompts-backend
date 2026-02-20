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
                .orElseThrow(() -> new BaseException(ModuleErrorCode.PRODUCTION_NOT_FOUND));
        
        // Presigned URL 생성
        String presignedUrl = generatePresignedUrl(artifact);
        String cdnUrl = artifactAccessService.generateCdnUrl(artifact.getS3Key());
        
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
        String s3Key = extractS3Key(artifact.getS3Key());
        String bucket = getBucketFromPath(artifact.getS3Key());
        
        return strategy.generatePresignedUrl(
                bucket,
                s3Key,
                contentType,
                Duration.ofSeconds(presignedUrlTtlSeconds)
        );
    }


    private String extractS3Key(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            return null;
        }
        if (s3Key.startsWith("s3://")) {
            String withoutPrefix = s3Key.substring(5);
            int slashIndex = withoutPrefix.indexOf('/');
            if (slashIndex > 0) {
                return withoutPrefix.substring(slashIndex + 1);
            }
        }
        return s3Key;
    }

    private String getBucketFromPath(String s3Key) {
        if (s3Key != null && s3Key.startsWith("s3://")) {
            String withoutPrefix = s3Key.substring(5);
            int slashIndex = withoutPrefix.indexOf('/');
            if (slashIndex > 0) {
                return withoutPrefix.substring(0, slashIndex);
            }
        }
        // 경로에서 추출할 수 없으면 설정값 사용
        return defaultBucket;
    }
}

