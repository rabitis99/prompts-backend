package org.example.sharedprompts.module.domain.production.service.artifact;

import org.example.sharedprompts.module.domain.production.entity.factory.ProductionArtifactDetailEntityFactory;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.application.storage.StorageFacade;
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
            String filePath
    ) {
        String content = new String(storageFacade.download(filePath), StandardCharsets.UTF_8);

        // TEXT 타입은 content를 직접 저장, s3Key는 null
        return ProductionArtifactDetailEntityFactory.createText(
                content,
                false // primary는 Aggregate Root에서 설정
        );
    }

    @Override
    public ArtifactDto toDto(ProductionArtifactDetailEntity detail) {
        // TEXT 타입은 항상 content를 가짐
        return new TextArtifactDto(ArtifactType.TEXT, detail.getContent());
    }
}
