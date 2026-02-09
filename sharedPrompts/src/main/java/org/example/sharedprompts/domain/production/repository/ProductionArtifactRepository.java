package org.example.sharedprompts.domain.production.repository;

import org.example.sharedprompts.domain.production.api.ArtifactType;
import org.example.sharedprompts.domain.production.api.ProductionCommandType;
import org.example.sharedprompts.domain.production.entity.ProductionArtifactEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductionArtifactRepository extends JpaRepository<ProductionArtifactEntity, Long> {
    
    Optional<ProductionArtifactEntity> findByProductionId(String productionId);
    
    List<ProductionArtifactEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    Page<ProductionArtifactEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    List<ProductionArtifactEntity> findByUserIdAndCommandTypeOrderByCreatedAtDesc(Long userId, ProductionCommandType commandType);
    
    Page<ProductionArtifactEntity> findByUserIdAndCommandTypeOrderByCreatedAtDesc(Long userId, ProductionCommandType commandType, Pageable pageable);
    
    List<ProductionArtifactEntity> findByUserIdAndArtifactTypeOrderByCreatedAtDesc(Long userId, ArtifactType artifactType);
    
    long countByUserId(Long userId);
    
    long countByUserIdAndCommandType(Long userId, ProductionCommandType commandType);
}

