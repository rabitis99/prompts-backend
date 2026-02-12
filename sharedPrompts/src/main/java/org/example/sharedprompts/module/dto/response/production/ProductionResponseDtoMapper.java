package org.example.sharedprompts.module.dto.response.production;

import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactEntity;
import org.example.sharedprompts.module.domain.production.service.artifact.ArtifactHandlerRegistry;
import org.example.sharedprompts.module.domain.production.service.production.ArtifactAccessService;

public final class ProductionResponseDtoMapper {

    private ProductionResponseDtoMapper() {
    }

    public static ProductionResponseDto toDto(
            ProductionArtifactEntity entity,
            ArtifactHandlerRegistry artifactHandlerRegistry) {

        ProductionStatus status = determineStatus(entity);

        ArtifactDto artifact = null;
        if (status == ProductionStatus.SUCCESS && entity.getDetail() != null) {
            artifact = ArtifactDtoMapper.toDto(entity.getDetail(), artifactHandlerRegistry);
        }

        return buildResponseDto(entity, status, artifact);
    }

    @Deprecated
    public static ProductionResponseDto toDto(ProductionArtifactEntity entity) {
        return toDto(entity, (ArtifactAccessService) null);
    }

    @Deprecated
    public static ProductionResponseDto toDto(
            ProductionArtifactEntity entity,
            ArtifactAccessService artifactAccessService) {

        ProductionStatus status = determineStatus(entity);

        ArtifactDto artifact = null;
        if (status == ProductionStatus.SUCCESS && entity.getDetail() != null) {
            artifact = ArtifactDtoMapper.toDto(entity.getDetail(), artifactAccessService);
        }

        return buildResponseDto(entity, status, artifact);
    }

    private static ProductionResponseDto buildResponseDto(
            ProductionArtifactEntity entity,
            ProductionStatus status,
            ArtifactDto artifact) {
        return new ProductionResponseDto(
                entity.getId(),
                status,
                status == ProductionStatus.FAILED && entity.getDetail() != null
                        ? entity.getDetail().getErrorMessage()
                        : null,
                entity.getStartedAt(),
                entity.getCompletedAt(),
                artifact
        );
    }

    private static ProductionStatus determineStatus(ProductionArtifactEntity entity) {
        if (!entity.isSuccess()) {
            return ProductionStatus.FAILED;
        }
        if (entity.getDetail() != null && entity.getDetail().getArtifactType() != null) {
            return ProductionStatus.SUCCESS;
        }
        return ProductionStatus.PROCESSING;
    }
}

