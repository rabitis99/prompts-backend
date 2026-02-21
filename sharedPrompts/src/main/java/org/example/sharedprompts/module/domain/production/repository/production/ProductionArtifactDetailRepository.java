package org.example.sharedprompts.module.domain.production.repository.production;

import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductionArtifactDetailRepository extends JpaRepository<ProductionArtifactDetailEntity, Long> {

    /**
     * 소유권 검증/Presigned URL 생성 시 N+1을 방지하기 위해 artifact(ProductionArtifactEntity)까지 함께 로딩합니다.
     */
    @EntityGraph(attributePaths = {"artifact"})
    Optional<ProductionArtifactDetailEntity> findByIdWithArtifact(Long id);
}

