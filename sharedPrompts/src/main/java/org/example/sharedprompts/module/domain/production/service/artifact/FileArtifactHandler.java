package org.example.sharedprompts.module.domain.production.service.artifact;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.example.sharedprompts.module.domain.production.service.production.ArtifactAccessService;
import org.example.sharedprompts.module.domain.production.service.storage.StorageStrategy;
import org.example.sharedprompts.module.domain.production.util.ArtifactMetadataHelper;
import org.example.sharedprompts.module.dto.response.production.ArtifactDto;
import org.example.sharedprompts.module.dto.response.production.FileArtifactDto;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileArtifactHandler implements ArtifactHandler {

    private final ArtifactAccessService artifactAccessService;

    @Override
    public ArtifactType getSupportedType() {
        return ArtifactType.FILE;
    }

    @Override
    public ProductionArtifactDetailEntity createDetail(
            JobEntity job, String filePath, StorageStrategy storageStrategy) {

        return ProductionArtifactDetailEntity.builder()
                .artifactType(ArtifactType.FILE)
                .storageType(ArtifactMetadataHelper.determineStorageFormat(filePath))
                .filePath(filePath)
                .fileName(ArtifactMetadataHelper.extractFileName(filePath))
                .contentType(ArtifactMetadataHelper.determineContentType(filePath))
                .storageLocation(storageStrategy.getStorageType().name())
                .build();
    }

    @Override
    public ArtifactDto toDto(ProductionArtifactDetailEntity detail) {
        String downloadUrl = artifactAccessService.generateDownloadUrl(detail.getFilePath());

        return new FileArtifactDto(
                ArtifactType.FILE,
                detail.getFilePath(),
                downloadUrl,
                detail.getFileName(),
                detail.getContentType(),
                detail.getStorageLocation()
        );
    }
}
