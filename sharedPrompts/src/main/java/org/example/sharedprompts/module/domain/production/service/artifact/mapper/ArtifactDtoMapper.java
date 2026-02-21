package org.example.sharedprompts.module.domain.production.service.artifact.mapper;

import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.example.sharedprompts.module.dto.response.production.ArtifactDto;

public interface ArtifactDtoMapper {
    ArtifactType getSupportedType();
    ArtifactDto toDto(ProductionArtifactDetailEntity detail);
}

