package org.example.sharedprompts.module.dto.response.production;

import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ArtifactSummaryDto 매퍼
 */
public final class ArtifactSummaryDtoMapper {

    private ArtifactSummaryDtoMapper() {
    }

    /**
     * ProductionArtifactDetailEntity를 ArtifactSummaryDto로 변환합니다.
     * 
     * @param detail 변환할 엔티티
     * @return 변환된 ArtifactSummaryDto
     */
    public static ArtifactSummaryDto toDto(ProductionArtifactDetailEntity detail) {
        if (detail == null) {
            return null;
        }
        return new ArtifactSummaryDto(
                detail.getId(),
                detail.getArtifactType(),
                detail.isPrimary(),
                detail.getFileName(),
                detail.getContentType()
        );
    }

    /**
     * ProductionArtifactDetailEntity 리스트를 ArtifactSummaryDto 리스트로 변환합니다.
     * 
     * @param details 변환할 엔티티 리스트
     * @return 변환된 ArtifactSummaryDto 리스트
     */
    public static List<ArtifactSummaryDto> toDtoList(List<ProductionArtifactDetailEntity> details) {
        if (details == null) {
            return List.of();
        }
        return details.stream()
                .map(ArtifactSummaryDtoMapper::toDto)
                .collect(Collectors.toList());
    }
}

