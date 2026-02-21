package org.example.sharedprompts.module.dto.response.production;

import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactEntity;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;
import org.example.sharedprompts.module.domain.production.model.production.ProductionStatus;
import org.example.sharedprompts.module.domain.production.service.artifact.ArtifactHandlerRegistry;
import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class ProductionResponseDtoMapper {

    private ProductionResponseDtoMapper() {
    }

    public static ProductionResponseDto toDto(
            ProductionArtifactEntity entity,
            ArtifactHandlerRegistry artifactHandlerRegistry) {
        return toDto(entity, null, artifactHandlerRegistry);
    }

    public static ProductionResponseDto toDto(
            ProductionArtifactEntity entity,
            @Nullable JobEntity jobEntity,
            ArtifactHandlerRegistry artifactHandlerRegistry) {

        // Job 정보가 있으면 Job 상태를 기반으로 결정, 없으면 Artifact 존재 여부로 결정
        ProductionStatus status;
        String errorMessage = null;
        Instant startedAt = null;
        Instant completedAt = null;

        if (jobEntity != null) {
            JobEntity job = jobEntity;
            JobStatus jobStatus = job.getStatus();
            
            // Job 상태를 ProductionStatus로 매핑
            status = switch (jobStatus) {
                case SUCCEEDED -> ProductionStatus.SUCCEEDED;
                case FAILED -> ProductionStatus.FAILED;
                case PENDING, RETRYING, PROCESSING, UNKNOWN -> ProductionStatus.PROCESSING;
            };
            
            errorMessage = job.getErrorMessage();
            startedAt = job.getStartedAt();
            completedAt = job.getCompletedAt();
        } else {
            // Job 정보가 없는 경우 (레거시 호환성): Artifact 존재 여부로 결정
            status = (entity.getArtifacts() != null && !entity.getArtifacts().isEmpty())
                    ? ProductionStatus.SUCCEEDED
                    : ProductionStatus.PROCESSING;
        }

        ArtifactDto artifact = null;
        List<ArtifactSummaryDto> artifacts = new ArrayList<>();
        
        // SUCCEEDED 상태일 때만 artifact 정보 반환
        // Job 정보가 없을 때는 이미 Artifact 존재 여부로 status를 결정했으므로 중복 체크 불필요
        // Job 정보가 있을 때를 대비한 방어적 null 체크만 수행
        if (status == ProductionStatus.SUCCEEDED) {
            // Job 정보가 있을 때 Artifact가 null일 수 있으므로 방어적 체크
            if (entity.getArtifacts() != null && !entity.getArtifacts().isEmpty()) {
            // primary artifact만 반환 (이미지 생성 시 PNG 이미지만 반환)
            ProductionArtifactDetailEntity primaryDetail = entity.getArtifacts().stream()
                    .filter(ProductionArtifactDetailEntity::isPrimary)
                    .findFirst()
                    .orElse(null);
            
                // primary artifact가 없으면 null 반환 (txt 파일만 있는 경우)
                if (primaryDetail != null) {
                    artifact = ArtifactDtoMapper.toDto(primaryDetail, artifactHandlerRegistry);
                }
                
                // 모든 artifacts를 summary로 변환
                artifacts = ArtifactSummaryDtoMapper.toDtoList(entity.getArtifacts());
            }
        }

        return buildResponseDto(entity, status, errorMessage, startedAt, completedAt, artifact, artifacts);
    }


    private static ProductionResponseDto buildResponseDto(
            ProductionArtifactEntity entity,
            ProductionStatus status,
            String errorMessage,
            Instant startedAt,
            Instant completedAt,
            ArtifactDto artifact,
            List<ArtifactSummaryDto> artifacts) {
        return new ProductionResponseDto(
                entity.getId(),
                status,
                errorMessage,
                startedAt,
                completedAt,
                artifact,
                artifacts
        );
    }
}

