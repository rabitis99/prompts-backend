package org.example.sharedprompts.module.domain.production.repository.production;

import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductionArtifactDetailRepository extends JpaRepository<ProductionArtifactDetailEntity, Long> {

    /**
     * 소유권 검증/Presigned URL 생성 시 N+1을 방지하기 위해 artifact(ProductionArtifactEntity)까지 함께 로딩합니다.
     */
    @EntityGraph(attributePaths = {"artifact"})
    @Query("SELECT d FROM ProductionArtifactDetailEntity d WHERE d.id = :id")
    Optional<ProductionArtifactDetailEntity> findByIdWithArtifact(@Param("id") Long id);

    /**
     * id·artifactId로 단일 detail 조회. artifact(ProductionArtifactEntity)까지 함께 로딩하여
     * 소유권 검증 등에서 N+1 없이 사용할 수 있습니다.
     */
    @EntityGraph(attributePaths = {"artifact"})
    Optional<ProductionArtifactDetailEntity> findByIdAndArtifact_Id(Long id, Long artifactId);
}

