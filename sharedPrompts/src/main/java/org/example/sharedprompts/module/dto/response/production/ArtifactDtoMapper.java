package org.example.sharedprompts.module.dto.response.production;

import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.entity.production.StorageFormat;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.example.sharedprompts.module.domain.production.service.production.ArtifactAccessService;

import java.util.Optional;

public class ArtifactDtoMapper {

    private ArtifactDtoMapper() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    public static ArtifactDto toDto(ProductionArtifactDetailEntity detail) {
        return toDto(detail, null);
    }

    public static ArtifactDto toDto(
            ProductionArtifactDetailEntity detail,
            ArtifactAccessService artifactAccessService) {
        
        ArtifactType artifactType = detail.getArtifactType();

        return switch (artifactType) {
            case TEXT -> {
                String content = detail.getStorageType() == StorageFormat.INLINE_TEXT
                        ? detail.getContent()
                        : null;
                yield new TextArtifactDto(ArtifactType.TEXT, content);
            }
            case IMAGE -> {
                String filePath = detail.getFilePath();
                String fileName = detail.getFileName();
                String contentType = detail.getContentType();
                String storageLocation = detail.getStorageLocation();
                String previewUrl = Optional.ofNullable(artifactAccessService)
                        .map(service -> service.generatePreviewUrl(filePath))
                        .orElse(null);
                yield new ImageArtifactDto(
                        ArtifactType.IMAGE, filePath, previewUrl, fileName, contentType, storageLocation);
            }
            case FILE -> {
                String filePath = detail.getFilePath();
                String fileName = detail.getFileName();
                String contentType = detail.getContentType();
                String storageLocation = detail.getStorageLocation();
                String downloadUrl = Optional.ofNullable(artifactAccessService)
                        .map(service -> service.generateDownloadUrl(filePath))
                        .orElse(null);
                yield new FileArtifactDto(
                        ArtifactType.FILE, filePath, downloadUrl, fileName, contentType, storageLocation);
            }
        };
    }
}

