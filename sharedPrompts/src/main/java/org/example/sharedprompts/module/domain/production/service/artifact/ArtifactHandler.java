package org.example.sharedprompts.module.domain.production.service.artifact;

import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.example.sharedprompts.module.domain.production.service.storage.StorageStrategy;
import org.example.sharedprompts.module.domain.production.util.ArtifactMetadataHelper;
import org.example.sharedprompts.module.dto.response.production.ArtifactDto;

public interface ArtifactHandler {

    ArtifactType getSupportedType();

    default ProductionArtifactDetailEntity createDetail(
            JobEntity job,
            String filePath,
            StorageStrategy storageStrategy
    ) {
        return ProductionArtifactDetailEntity.builder()
                .artifactType(getSupportedType())
                .storageType(ArtifactMetadataHelper.determineStorageFormat(filePath))
                .filePath(filePath)
                .fileName(ArtifactMetadataHelper.extractFileName(filePath))
                .contentType(ArtifactMetadataHelper.determineContentType(filePath))
                .storageLocation(storageStrategy.getStorageType().name())
                .build();
    }

    ArtifactDto toDto(ProductionArtifactDetailEntity detail);
}
