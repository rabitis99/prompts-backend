package org.example.sharedprompts.module.domain.production.repository;

import org.example.sharedprompts.module.domain.production.api.artifact.ArtifactType;
import org.example.sharedprompts.module.domain.production.api.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.entity.ProductionArtifactEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionArtifactRepository 
        extends JpaRepository<ProductionArtifactEntity, Long>, CustomProductionArtifactRepository {
    
    long countByUserId(Long userId);
    long countByUserIdAndCommandType(Long userId, ProductionCommandType commandType);
    long countByUserIdAndArtifactType(Long userId, ArtifactType artifactType);
}

