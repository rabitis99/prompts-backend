package org.example.sharedprompts.module.domain.production.repository.production;

import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductionArtifactRepository extends JpaRepository<ProductionArtifactEntity, Long> {
    
    @EntityGraph(attributePaths = {"detail"})
    @Query("SELECT a FROM ProductionArtifactEntity a WHERE a.id = :id")
    Optional<ProductionArtifactEntity> findByIdWithDetail(@Param("id") Long id);
    
    @Query("SELECT p FROM ProductionArtifactEntity p LEFT JOIN FETCH p.artifacts WHERE p.id = :id")
    Optional<ProductionArtifactEntity> findByIdWithArtifacts(@Param("id") Long id);
}

