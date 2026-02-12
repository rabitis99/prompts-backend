package org.example.sharedprompts.module.domain.production.service.artifact;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.entity.production.StorageFormat;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.example.sharedprompts.module.domain.production.service.storage.StorageStrategy;
import org.example.sharedprompts.module.domain.production.util.ArtifactMetadataHelper;
import org.example.sharedprompts.module.dto.response.production.ArtifactDto;
import org.example.sharedprompts.module.dto.response.production.TextArtifactDto;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TextArtifactHandler implements ArtifactHandler {

    @Override
    public ArtifactType getSupportedType() {
        return ArtifactType.TEXT;
    }

    @Override
    public ProductionArtifactDetailEntity createDetail(
            JobEntity job, String filePath, StorageStrategy storageStrategy) {

        return ProductionArtifactDetailEntity.builder()
                .artifactType(ArtifactType.TEXT)
                .storageType(ArtifactMetadataHelper.determineStorageFormat(filePath))
                .filePath(filePath)
                .fileName(ArtifactMetadataHelper.extractFileName(filePath))
                .contentType(ArtifactMetadataHelper.determineContentType(filePath))
                .storageLocation(storageStrategy.getStorageType().name())
                .build();
    }

    @Override
    public ArtifactDto toDto(ProductionArtifactDetailEntity detail) {
        String content = detail.getStorageType() == StorageFormat.INLINE_TEXT
                ? detail.getContent()
                : null;
        return new TextArtifactDto(ArtifactType.TEXT, content);
    }
}
