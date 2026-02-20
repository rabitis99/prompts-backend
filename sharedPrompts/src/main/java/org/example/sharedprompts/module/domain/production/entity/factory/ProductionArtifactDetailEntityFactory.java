package org.example.sharedprompts.module.domain.production.entity.factory;

import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;

/**
 * ProductionArtifactDetailEntity Factory
 * 엔티티 생성 로직과 불변 조건 검증을 담당합니다.
 */
public final class ProductionArtifactDetailEntityFactory {

    private ProductionArtifactDetailEntityFactory() {
    }

    /**
     * TEXT 타입 전용 생성 메서드
     * 불변 조건: content 필수, s3Key는 null
     * primary는 항상 false로 생성되며, Aggregate Root(ProductionArtifactEntity.markAsPrimary())에서만 설정됩니다.
     */
    public static ProductionArtifactDetailEntity createText(
            String content
    ) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content cannot be null or blank for TEXT type");
        }
        
        return ProductionArtifactDetailEntity.builder()
                .artifactType(ArtifactType.TEXT)
                .content(content)
                .s3Key(null)
                .build();
    }

    /**
     * FILE/IMAGE 타입 전용 생성 메서드
     * 불변 조건: s3Key 필수, contentType 필수, content는 null
     * primary는 항상 false로 생성되며, Aggregate Root(ProductionArtifactEntity.markAsPrimary())에서만 설정됩니다.
     * 
     * @param actualImagePath 실제 이미지 파일 경로 (HTML 파일인 경우 내부 이미지 경로, null이면 s3Key 사용)
     */
    public static ProductionArtifactDetailEntity createFile(
            ArtifactType artifactType,
            String s3Key,
            String fileName,
            String contentType,
            Long fileSize,
            String actualImagePath
    ) {
        if (artifactType != ArtifactType.FILE && artifactType != ArtifactType.IMAGE) {
            throw new IllegalArgumentException(
                String.format("ArtifactType must be FILE or IMAGE, but was %s", artifactType)
            );
        }
        if (s3Key == null || s3Key.isBlank()) {
            throw new IllegalArgumentException("S3Key cannot be null or blank for FILE/IMAGE type");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("ContentType cannot be null or blank for FILE/IMAGE type");
        }
        
        return ProductionArtifactDetailEntity.builder()
                .artifactType(artifactType)
                .s3Key(s3Key)
                .fileName(fileName)
                .contentType(contentType)
                .fileSize(fileSize)
                .content(null)
                .actualImagePath(actualImagePath != null ? actualImagePath : s3Key)
                .build();
    }

    /**
     * IMAGE 타입 전용 생성 메서드 (편의 메서드)
     * 
     * @param actualImagePath 실제 이미지 파일 경로 (HTML 파일인 경우 내부 이미지 경로, null이면 s3Key 사용)
     */
    public static ProductionArtifactDetailEntity createImage(
            String s3Key,
            String fileName,
            String contentType,
            Long fileSize,
            String actualImagePath
    ) {
        return createFile(ArtifactType.IMAGE, s3Key, fileName, contentType, fileSize, actualImagePath);
    }
}

