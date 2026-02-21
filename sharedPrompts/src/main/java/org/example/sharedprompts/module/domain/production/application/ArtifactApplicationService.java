package org.example.sharedprompts.module.domain.production.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactEntity;
import org.example.sharedprompts.module.domain.production.application.presign.PresignedUrlService;
import org.example.sharedprompts.module.domain.production.service.production.access.ArtifactOwnershipValidator;
import org.example.sharedprompts.module.domain.production.service.production.ArtifactAccessService;
import org.example.sharedprompts.module.dto.response.production.ArtifactDetailResponseDto;
import org.example.sharedprompts.module.dto.response.production.ArtifactDetailResponseDtoMapper;
import org.example.sharedprompts.module.dto.response.production.ArtifactSummaryDto;
import org.example.sharedprompts.module.dto.response.production.ArtifactSummaryDtoMapper;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArtifactApplicationService {

    private final PresignedUrlService presignedUrlService;
    private final ArtifactAccessService artifactAccessService;
    private final ArtifactOwnershipValidator ownershipValidator;

    @Value("${artifact.url.default-ttl:300}")
    private int presignedUrlTtlSeconds;
    
    @Transactional(readOnly = true)
    public List<ArtifactSummaryDto> getArtifacts(Long productionId, Long userId) {
        log.info("Artifacts requested - productionId: {}, userId: {}", productionId, userId);

        ProductionArtifactEntity production = ownershipValidator.validateProductionOwner(productionId, userId);
        
        return ArtifactSummaryDtoMapper.toDtoList(production.getArtifacts());
    }

    @Transactional(readOnly = true)
    public ArtifactDetailResponseDto getArtifact(Long productionId, Long artifactId, Long userId) {
        log.info("Artifact detail requested - productionId: {}, artifactId: {}, userId: {}", 
                productionId, artifactId, userId);

        ProductionArtifactEntity production = ownershipValidator.validateProductionOwner(productionId, userId);
        
        ProductionArtifactDetailEntity artifact = production.getArtifacts().stream()
                .filter(detail -> detail.getId().equals(artifactId))
                .findFirst()
                .orElseThrow(() -> new BaseException(ModuleErrorCode.ARTIFACT_NOT_FOUND));
        
        // Presigned URL 생성
        String presignedUrl = presignedUrlService.generateForArtifact(
                artifact,
                java.time.Duration.ofSeconds(presignedUrlTtlSeconds)
        );
        
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

}

