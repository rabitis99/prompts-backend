package org.example.sharedprompts.module.dto.response.production;

import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.entity.production.StorageFormat;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.example.sharedprompts.module.domain.production.service.production.ArtifactAccessService;

import java.util.Optional;

public class ArtifactDtoMapper {

    public static ArtifactDto toDto(ProductionArtifactDetailEntity detail) {
        return toDto(detail, null);
    }

    public static ArtifactDto toDto(
            ProductionArtifactDetailEntity detail,
            ArtifactAccessService artifactAccessService) {
        
        ArtifactType artifactType = detail.getArtifactType();
        String fileName = detail.getFileName();
        String contentType = detail.getContentType();
        String storageLocation = detail.getStorageLocation();

        if (artifactType == ArtifactType.TEXT) {
            String content = detail.getStorageType() == StorageFormat.INLINE_TEXT
                    ? detail.getContent()
                    : null;
            return new TextArtifactDto(
                    ArtifactType.TEXT,
                    content,
                    fileName,
                    contentType,
                    storageLocation
            );
        } else if (artifactType == ArtifactType.IMAGE) {
            String filePath = detail.getFilePath();
            String previewUrl = Optional.ofNullable(artifactAccessService)
                    .map(service -> service.generatePreviewUrl(filePath))
                    .orElse(null);
            return new ImageArtifactDto(
                    ArtifactType.IMAGE,
                    filePath,
                    previewUrl,
                    fileName,
                    contentType,
                    storageLocation
            );
        } else if (artifactType == ArtifactType.FILE) {
            String filePath = detail.getFilePath();
            String downloadUrl = Optional.ofNullable(artifactAccessService)
                    .map(service -> service.generateDownloadUrl(filePath))
                    .orElse(null);
            return new FileArtifactDto(
                    ArtifactType.FILE,
                    filePath,
                    downloadUrl,
                    fileName,
                    contentType,
                    storageLocation
            );
        } else {
            throw new IllegalArgumentException("Unknown artifact type: " + artifactType);
        }
    }
}

