package org.example.sharedprompts.module.domain.production.service.artifact;

import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.entity.production.StorageFormat;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.application.storage.StorageFacade;
import org.example.sharedprompts.module.domain.production.service.storage.StorageStrategy;
import org.example.sharedprompts.module.domain.production.util.ArtifactMetadataHelper;
import org.example.sharedprompts.module.dto.response.production.ArtifactDto;
import org.example.sharedprompts.module.dto.response.production.TextArtifactDto;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class TextArtifactHandler implements ArtifactHandler {

    private final StorageFacade storageFacade;

    @Override
    public ArtifactType getSupportedType() {
        return ArtifactType.TEXT;
    }

    @Override
    public ProductionArtifactDetailEntity createDetail(
            String filePath,
            StorageStrategy storageStrategy
    ) {
        String content = new String(storageFacade.download(filePath), StandardCharsets.UTF_8);

        return ProductionArtifactDetailEntity.builder()
                .artifactType(getSupportedType())
                .storageType(StorageFormat.INLINE_TEXT)
                .content(content)
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
