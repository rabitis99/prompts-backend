package org.example.sharedprompts.module.domain.production.service.artifact;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.example.sharedprompts.module.domain.production.service.production.ArtifactAccessService;
import org.example.sharedprompts.module.dto.response.production.ArtifactDto;
import org.example.sharedprompts.module.dto.response.production.ImageArtifactDto;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageArtifactHandler implements ArtifactHandler {

    private final ArtifactAccessService artifactAccessService;

    @Override
    public ArtifactType getSupportedType() {
        return ArtifactType.IMAGE;
    }


    @Override
    public ArtifactDto toDto(ProductionArtifactDetailEntity detail) {
        String previewUrl = artifactAccessService.generatePreviewUrl(detail.getFilePath());

        return new ImageArtifactDto(
                ArtifactType.IMAGE,
                detail.getFilePath(),
                previewUrl,
                detail.getFileName(),
                detail.getContentType(),
                detail.getStorageLocation()
        );
    }
}
