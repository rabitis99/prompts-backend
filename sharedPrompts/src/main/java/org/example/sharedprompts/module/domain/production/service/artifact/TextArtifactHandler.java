package org.example.sharedprompts.module.domain.production.service.artifact;

import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.entity.production.StorageFormat;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.example.sharedprompts.module.dto.response.production.ArtifactDto;
import org.example.sharedprompts.module.dto.response.production.TextArtifactDto;
import org.springframework.stereotype.Component;

@Component
public class TextArtifactHandler implements ArtifactHandler {

    @Override
    public ArtifactType getSupportedType() {
        return ArtifactType.TEXT;
    }

    @Override
    public ArtifactDto toDto(ProductionArtifactDetailEntity detail) {
        String content = detail.getStorageType() == StorageFormat.INLINE_TEXT
                ? detail.getContent()
                : null;
        return new TextArtifactDto(ArtifactType.TEXT, content);
    }
}
