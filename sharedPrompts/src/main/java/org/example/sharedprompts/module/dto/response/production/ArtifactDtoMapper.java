package org.example.sharedprompts.module.dto.response.production;

import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.service.artifact.ArtifactHandlerRegistry;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;

/**
 * Artifact DTO 매퍼
 * ArtifactHandlerRegistry를 통해 적절한 Handler를 사용하여 DTO를 생성합니다.
 */
public class ArtifactDtoMapper {

    private ArtifactDtoMapper() {
    }

    /**
     * ProductionArtifactDetailEntity를 ArtifactDto로 변환합니다.
     * 
     * @param detail 변환할 엔티티
     * @param registry ArtifactHandlerRegistry
     * @return 변환된 ArtifactDto
     */
    public static ArtifactDto toDto(
            ProductionArtifactDetailEntity detail,
            ArtifactHandlerRegistry registry) {
        if (detail == null) {
            throw new BaseException(ModuleErrorCode.VALIDATION_ERROR, "detail",
                    "ProductionArtifactDetailEntity must not be null");
        }
        if (registry == null) {
            throw new BaseException(ModuleErrorCode.VALIDATION_ERROR, "registry",
                    "ArtifactHandlerRegistry must not be null");
        }
        return registry.getHandler(detail.getArtifactType()).toDto(detail);
    }
}

