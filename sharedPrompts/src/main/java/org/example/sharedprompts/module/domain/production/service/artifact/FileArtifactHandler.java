package org.example.sharedprompts.module.domain.production.service.artifact;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.example.sharedprompts.module.domain.production.service.production.ArtifactAccessService;
import org.example.sharedprompts.module.dto.response.production.ArtifactDto;
import org.example.sharedprompts.module.dto.response.production.FileArtifactDto;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileArtifactHandler implements ArtifactHandler {

    private final ArtifactAccessService artifactAccessService;

    @Override
    public ArtifactType getSupportedType() {
        return ArtifactType.FILE;
    }


    @Override
    public ArtifactDto toDto(ProductionArtifactDetailEntity detail) {
        String downloadUrl = artifactAccessService.generateDownloadUrl(detail.getFilePath());
        String cdnUrl = artifactAccessService.generateCdnUrl(detail.getFilePath());

        return new FileArtifactDto(
                ArtifactType.FILE,
                detail.getFilePath(),
                downloadUrl,
                detail.getFileName(),
                detail.getContentType(),
                detail.getStorageLocation(),
                cdnUrl
        );
    }
}
