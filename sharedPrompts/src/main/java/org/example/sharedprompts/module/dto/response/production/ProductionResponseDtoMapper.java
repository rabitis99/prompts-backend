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

        ProductionStatus status = determineStatus(entity);

        ArtifactDto artifact = null;
        List<ArtifactSummaryDto> artifacts = new ArrayList<>();
        
        if (status == ProductionStatus.SUCCESS && entity.getArtifacts() != null && !entity.getArtifacts().isEmpty()) {
            // primary artifact만 반환 (이미지 생성 시 PNG 이미지만 반환)
            ProductionArtifactDetailEntity primaryDetail = entity.getArtifacts().stream()
                    .filter(ProductionArtifactDetailEntity::getIsPrimary)
                    .findFirst()
                    .orElse(null);
            
            // primary artifact가 없으면 null 반환 (txt 파일만 있는 경우)
            if (primaryDetail != null) {
                artifact = ArtifactDtoMapper.toDto(primaryDetail, artifactHandlerRegistry);
            }
            
            // 모든 artifacts를 summary로 변환
            artifacts = entity.getArtifacts().stream()
                    .map(detail -> new ArtifactSummaryDto(
                            detail.getId(),
                            detail.getArtifactType(),
                            detail.getIsPrimary(),
                            detail.getFileName(),
                            detail.getContentType()
                    ))
                    .toList();
        }

        return buildResponseDto(entity, status, artifact, artifacts);
    }


    private static ProductionResponseDto buildResponseDto(
            ProductionArtifactEntity entity,
            ProductionStatus status,
            ArtifactDto artifact,
            List<ArtifactSummaryDto> artifacts) {
        String errorMessage = null;
        if (status == ProductionStatus.FAILED && entity.getArtifacts() != null && !entity.getArtifacts().isEmpty()) {
            errorMessage = entity.getArtifacts().get(0).getErrorMessage();
        } else if (status == ProductionStatus.FAILED && entity.getDetail() != null) {
            errorMessage = entity.getDetail().getErrorMessage();
        }
        
        return new ProductionResponseDto(
                entity.getId(),
                status,
                errorMessage,
                entity.getStartedAt(),
                entity.getCompletedAt(),
                artifact,
                artifacts
        );
    }

    private static ProductionStatus determineStatus(ProductionArtifactEntity entity) {
        if (!entity.isSuccess()) {
            return ProductionStatus.FAILED;
        }
        if (entity.getArtifacts() != null && !entity.getArtifacts().isEmpty()) {
            return ProductionStatus.SUCCESS;
        }
        // 기존 호환성
        if (entity.getDetail() != null && entity.getDetail().getArtifactType() != null) {
            return ProductionStatus.SUCCESS;
        }
        return ProductionStatus.PROCESSING;
    }
}

