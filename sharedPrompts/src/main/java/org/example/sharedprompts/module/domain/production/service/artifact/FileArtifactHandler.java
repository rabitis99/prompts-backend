package org.example.sharedprompts.module.domain.production.service.artifact;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.example.sharedprompts.module.domain.production.service.artifact.mapper.FileArtifactMapper;
import org.example.sharedprompts.module.dto.response.production.ArtifactDto;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileArtifactHandler implements ArtifactHandler {

    private final FileArtifactMapper fileArtifactMapper;

    @Override
    public ArtifactType getSupportedType() {
        return ArtifactType.FILE;
    }

    @Override
    public ArtifactDto toDto(ProductionArtifactDetailEntity detail) {
        return fileArtifactMapper.toDto(detail);
    }
}
