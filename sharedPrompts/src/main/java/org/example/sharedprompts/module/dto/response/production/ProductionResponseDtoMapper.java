package org.example.sharedprompts.module.dto.response.production;

import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactEntity;
import org.example.sharedprompts.module.domain.production.service.artifact.ArtifactHandlerRegistry;

import java.util.ArrayList;
import java.util.List;

public final class ProductionResponseDtoMapper {

    private ProductionResponseDtoMapper() {
    }

    public static ProductionResponseDto toDto(
            ProductionArtifactEntity entity,
            ArtifactHandlerRegistry artifactHandlerRegistry) {

        // Artifact는 Job 성공 시에만 생성되므로, Artifact가 존재하면 SUCCESS
        ProductionStatus status = (entity.getArtifacts() != null && !entity.getArtifacts().isEmpty())
                ? ProductionStatus.SUCCESS
                : ProductionStatus.PROCESSING;

        ArtifactDto artifact = null;
        List<ArtifactSummaryDto> artifacts = new ArrayList<>();
        
        if (status == ProductionStatus.SUCCESS && entity.getArtifacts() != null && !entity.getArtifacts().isEmpty()) {
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

        return buildResponseDto(entity, status, artifact, artifacts);
    }


    private static ProductionResponseDto buildResponseDto(
            ProductionArtifactEntity entity,
            ProductionStatus status,
            ArtifactDto artifact,
            List<ArtifactSummaryDto> artifacts) {
        // errorMessage는 JobFailureLog에서 관리 (Artifact는 결과 의미 단위이므로 에러 정보 보유하지 않음)
        // startedAt, completedAt은 Job에서 관리 (Artifact는 createdAt만 보유)
        
        return new ProductionResponseDto(
                entity.getId(),
                status,
                null, // errorMessage는 Job에서 관리
                null, // startedAt은 Job에서 관리
                null, // completedAt은 Job에서 관리
                artifact,
                artifacts
        );
    }
}

