package org.example.sharedprompts.module.dto.response.production;

import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.entity.production.StorageFormat;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.example.sharedprompts.module.domain.production.service.production.ArtifactAccessService;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;

import java.util.Optional;

public class ArtifactDtoMapper {

    private ArtifactDtoMapper() {
    }

    public static ArtifactDto toDto(ProductionArtifactDetailEntity detail) {
        return toDto(detail, null);
    }

    public static ArtifactDto toDto(
            ProductionArtifactDetailEntity detail,
            ArtifactAccessService artifactAccessService) {
        
        if (detail == null) {
            throw new BaseException(ModuleErrorCode.VALIDATION_ERROR, "detail", "ProductionArtifactDetailEntity must not be null");
        }
        ArtifactType artifactType = detail.getArtifactType();

        return switch (artifactType) {
            case TEXT -> {
                String content = detail.getStorageType() == StorageFormat.INLINE_TEXT
                        ? detail.getContent()
                        : null;
                yield new TextArtifactDto(ArtifactType.TEXT, content);
            }
            case IMAGE -> {
                FileMetadata metadata = extractFileMetadata(detail);
                String previewUrl = Optional.ofNullable(artifactAccessService)
                        .map(service -> service.generatePreviewUrl(metadata.filePath()))
                        .orElse(null);
                yield new ImageArtifactDto(
                        ArtifactType.IMAGE, metadata.filePath(), previewUrl, 
                        metadata.fileName(), metadata.contentType(), metadata.storageLocation());
            }
            case FILE -> {
                FileMetadata metadata = extractFileMetadata(detail);
                String downloadUrl = Optional.ofNullable(artifactAccessService)
                        .map(service -> service.generateDownloadUrl(metadata.filePath()))
                        .orElse(null);
                yield new FileArtifactDto(
                        ArtifactType.FILE, metadata.filePath(), downloadUrl, 
                        metadata.fileName(), metadata.contentType(), metadata.storageLocation());
            }
        };
    }

    private static FileMetadata extractFileMetadata(ProductionArtifactDetailEntity detail) {
        return new FileMetadata(
                detail.getFilePath(),
                detail.getFileName(),
                detail.getContentType(),
                detail.getStorageLocation()
        );
    }

    private record FileMetadata(
            String filePath,
            String fileName,
            String contentType,
            String storageLocation
    ) {
    }
}

