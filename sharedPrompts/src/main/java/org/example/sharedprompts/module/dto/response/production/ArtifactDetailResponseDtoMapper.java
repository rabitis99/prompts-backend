package org.example.sharedprompts.module.dto.response.production;

import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactEntity;

import java.time.ZoneOffset;
import java.util.Map;

/**
 * ArtifactDetailResponseDto 매퍼
 */
public final class ArtifactDetailResponseDtoMapper {

    private ArtifactDetailResponseDtoMapper() {
    }

    /**
     * ProductionArtifactDetailEntity를 ArtifactDetailResponseDto로 변환합니다.
     * 
     * @param artifact ProductionArtifactEntity (productionId를 위해 필요)
     * @param detail 변환할 ProductionArtifactDetailEntity
     * @param presignedUrl Presigned URL (null 가능)
     * @param cdnUrl CDN URL (null 가능)
     * @param thumbnailUrls 썸네일 URL 맵 (null 가능)
     * @param content 인라인 본문 (TEXT 등 DB content 사용 시, null 가능)
     * @return 변환된 ArtifactDetailResponseDto
     */
    public static ArtifactDetailResponseDto toDto(
            ProductionArtifactEntity artifact,
            ProductionArtifactDetailEntity detail,
            String presignedUrl,
            String cdnUrl,
            Map<String, String> thumbnailUrls,
            String content
    ) {
        if (detail == null) {
            return null;
        }
        if (artifact == null) {
            throw new IllegalArgumentException("ProductionArtifactEntity cannot be null");
        }
        
        return new ArtifactDetailResponseDto(
                detail.getId(),
                artifact.getId(),
                detail.getArtifactType(),
                detail.isPrimary(),
                detail.getFileName(),
                detail.getContentType(),
                "S3", // storageLocation은 항상 S3
                presignedUrl,
                cdnUrl,
                thumbnailUrls,
                content,
                detail.getCreatedAt() != null
                        ? detail.getCreatedAt().toInstant(ZoneOffset.UTC)
                        : null
        );
    }
}

