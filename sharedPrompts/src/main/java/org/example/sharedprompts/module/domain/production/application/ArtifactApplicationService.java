package org.example.sharedprompts.module.domain.production.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactEntity;
import org.example.sharedprompts.module.domain.production.repository.production.ProductionArtifactDetailRepository;
import org.example.sharedprompts.module.domain.production.repository.production.ProductionArtifactRepository;
import org.example.sharedprompts.module.domain.production.service.production.ArtifactAccessService;
import org.example.sharedprompts.module.domain.production.service.production.presign.PresignedStrategy;
import org.example.sharedprompts.module.domain.production.service.production.presign.PresignedStrategyResolver;
import org.example.sharedprompts.module.dto.response.production.ArtifactDetailResponseDto;
import org.example.sharedprompts.module.dto.response.production.ArtifactSummaryDto;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArtifactApplicationService {

    private final ProductionArtifactRepository productionArtifactRepository;
    private final ProductionArtifactDetailRepository artifactDetailRepository;
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
        
        return production.getArtifacts().stream()
                .map(detail -> new ArtifactSummaryDto(
                        detail.getId(),
                        detail.getArtifactType(),
                        detail.getIsPrimary(),
                        detail.getFileName(),
                        detail.getContentType()
                ))
                .collect(Collectors.toList());
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
        String cdnUrl = artifactAccessService.generateCdnUrl(artifact.getFilePath());
        
        return new ArtifactDetailResponseDto(
                artifact.getId(),
                production.getId(),
                artifact.getArtifactType(),
                artifact.getIsPrimary(),
                artifact.getFileName(),
                artifact.getContentType(),
                artifact.getStorageLocation(),
                presignedUrl,
                cdnUrl,
                null, // thumbnailUrls는 별도 처리 필요
                artifact.getCreatedAt()
        );
    }

    private String generatePresignedUrl(ProductionArtifactDetailEntity artifact) {
        if (artifact.getFilePath() == null || artifact.getFilePath().isBlank()) {
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
        
        // DB에 저장된 filePath가 실제 S3 key (또는 s3:// 형식)
        String s3Key = extractS3Key(artifact.getFilePath());
        String bucket = getBucketFromPath(artifact.getFilePath());
        
        return strategy.generatePresignedUrl(
                bucket,
                s3Key,
                contentType,
                Duration.ofSeconds(presignedUrlTtlSeconds)
        );
    }


    private String extractS3Key(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }
        if (filePath.startsWith("s3://")) {
            String withoutPrefix = filePath.substring(5);
            int slashIndex = withoutPrefix.indexOf('/');
            if (slashIndex > 0) {
                return withoutPrefix.substring(slashIndex + 1);
            }
        }
        return filePath;
    }

    private String getBucketFromPath(String filePath) {
        if (filePath != null && filePath.startsWith("s3://")) {
            String withoutPrefix = filePath.substring(5);
            int slashIndex = withoutPrefix.indexOf('/');
            if (slashIndex > 0) {
                return withoutPrefix.substring(0, slashIndex);
            }
        }
        // 경로에서 추출할 수 없으면 설정값 사용
        return defaultBucket;
    }
}

