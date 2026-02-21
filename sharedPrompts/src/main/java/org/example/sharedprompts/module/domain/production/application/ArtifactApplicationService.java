package org.example.sharedprompts.module.domain.production.application;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class ArtifactApplicationService {

    private final PresignedUrlService presignedUrlService;
    private final ArtifactAccessService artifactAccessService;
    private final ArtifactOwnershipValidator ownershipValidator;
    private final int presignedUrlTtlSeconds;

    public ArtifactApplicationService(
            PresignedUrlService presignedUrlService,
            ArtifactAccessService artifactAccessService,
            ArtifactOwnershipValidator ownershipValidator,
            @Value("${artifact.url.default-ttl:900}") int presignedUrlTtlSeconds) {
        this.presignedUrlService = presignedUrlService;
        this.artifactAccessService = artifactAccessService;
        this.ownershipValidator = ownershipValidator;
        this.presignedUrlTtlSeconds = presignedUrlTtlSeconds;
    }
    
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

        ProductionArtifactDetailEntity artifact = ownershipValidator.getArtifactByProductionAndId(productionId, artifactId, userId);
        ProductionArtifactEntity production = artifact.getArtifact();

        // Presigned URL 생성
        String presignedUrl = presignedUrlService.generateForArtifact(
                artifact,
                Duration.ofSeconds(presignedUrlTtlSeconds)
        );

        // CDN URL 생성 (TEXT 타입은 s3Key가 null일 수 있음)
        String s3Key = artifact.getS3Key();
        String cdnUrl = (s3Key != null && !s3Key.isBlank())
                ? artifactAccessService.generateCdnUrl(s3Key)
                : null;

        // TEXT/EMAIL 등 DB content가 있으면 응답에 포함하여 클라이언트가 본문에 접근할 수 있게 함
        String content = (artifact.getContent() != null && !artifact.getContent().isBlank())
                ? artifact.getContent()
                : null;

        return ArtifactDetailResponseDtoMapper.toDto(
                production,
                artifact,
                presignedUrl,
                cdnUrl,
                Collections.emptyMap(), // thumbnailUrls 미구현, 빈 맵으로 명시
                content
        );
    }

}

