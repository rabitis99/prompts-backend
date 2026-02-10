package org.example.sharedprompts.module.domain.production.repository;

import org.example.sharedprompts.module.domain.production.api.artifact.ArtifactType;
import org.example.sharedprompts.module.domain.production.api.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.entity.ProductionArtifactEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomProductionArtifactRepository {
    Page<ProductionArtifactEntity> findPageByUserId(Long userId, Pageable pageable);
    Page<ProductionArtifactEntity> findPageByUserIdAndCommandType(Long userId, ProductionCommandType commandType, Pageable pageable);
    Page<ProductionArtifactEntity> findPageByUserIdAndArtifactType(Long userId, ArtifactType artifactType, Pageable pageable);
    Long findLatestIdByUserIdAndCommandType(Long userId, ProductionCommandType commandType);
}

