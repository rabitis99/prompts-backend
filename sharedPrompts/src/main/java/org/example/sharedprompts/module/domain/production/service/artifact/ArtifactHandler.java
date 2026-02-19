package org.example.sharedprompts.module.domain.production.service.artifact;

import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.example.sharedprompts.module.domain.production.service.storage.StorageType;
import org.example.sharedprompts.module.domain.production.util.ArtifactMetadataHelper;
import org.example.sharedprompts.module.dto.response.production.ArtifactDto;

public interface ArtifactHandler {

    ArtifactType getSupportedType();

    default ProductionArtifactDetailEntity createDetail(
            String filePath
    ) {
        return ProductionArtifactDetailEntity.builder()
                .artifactType(getSupportedType())
                .storageType(ArtifactMetadataHelper.determineStorageFormat(filePath))
                .filePath(filePath)
                .fileName(ArtifactMetadataHelper.extractFileName(filePath))
                .contentType(ArtifactMetadataHelper.determineContentType(filePath))
                .storageLocation(StorageType.S3.name())
                .build();
    }

    ArtifactDto toDto(ProductionArtifactDetailEntity detail);
}
