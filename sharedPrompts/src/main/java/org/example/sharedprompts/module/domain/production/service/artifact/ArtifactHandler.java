package org.example.sharedprompts.module.domain.production.service.artifact;

import org.example.sharedprompts.module.domain.production.entity.factory.ProductionArtifactDetailEntityFactory;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.example.sharedprompts.module.domain.production.util.ArtifactMetadataHelper;
import org.example.sharedprompts.module.dto.response.production.ArtifactDto;

public interface ArtifactHandler {

    ArtifactType getSupportedType();

    /**
     * FILE/IMAGE 타입용 기본 구현
     * TEXT 타입은 TextArtifactHandler에서 오버라이드
     */
    default ProductionArtifactDetailEntity createDetail(
            String s3Key
    ) {
        String fileName = ArtifactMetadataHelper.extractFileName(s3Key);
        String contentType = ArtifactMetadataHelper.determineContentType(s3Key);
        
        return ProductionArtifactDetailEntityFactory.createFile(
                getSupportedType(),
                s3Key,
                fileName,
                contentType,
                null, // fileSize는 나중에 설정 가능
                null  // actualImagePath는 null (s3Key 사용)
        );
    }

    ArtifactDto toDto(ProductionArtifactDetailEntity detail);
}
