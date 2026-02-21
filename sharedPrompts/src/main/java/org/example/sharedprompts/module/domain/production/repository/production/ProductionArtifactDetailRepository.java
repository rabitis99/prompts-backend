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
     * productionId·artifactId로 단일 detail 직접 조회 (아티팩트 전체 로드 없이 조회).
     */
    @EntityGraph(attributePaths = {"artifact"})
    Optional<ProductionArtifactDetailEntity> findByIdAndArtifact_Id(Long id, Long artifactId);
}

