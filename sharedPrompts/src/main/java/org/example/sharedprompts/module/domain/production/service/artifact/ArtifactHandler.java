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
            String filePath
    ) {
        // filePath는 S3 key로 사용
        String fileName = ArtifactMetadataHelper.extractFileName(filePath);
        String contentType = ArtifactMetadataHelper.determineContentType(filePath);
        
        return ProductionArtifactDetailEntityFactory.createFile(
                getSupportedType(),
                filePath, // s3Key
                fileName,
                contentType,
                null, // fileSize는 나중에 설정 가능
                false // primary는 Aggregate Root에서 설정
        );
    }

    ArtifactDto toDto(ProductionArtifactDetailEntity detail);
}
