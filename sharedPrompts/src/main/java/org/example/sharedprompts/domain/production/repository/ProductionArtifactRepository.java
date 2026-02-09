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
    
    // ⚠️ 대량 데이터 로딩 위험: 최대 1000건만 반환 (Top1000 사용)
    // 사용자의 artifact 수가 증가하면 메모리 이슈와 쿼리 성능 저하가 발생할 수 있습니다.
    // 가능하면 Page variant를 사용하는 것을 권장합니다.
    List<ProductionArtifactEntity> findTop1000ByUserIdOrderByCreatedAtDesc(Long userId);
    
    Page<ProductionArtifactEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    // ⚠️ 대량 데이터 로딩 위험: 최대 1000건만 반환 (Top1000 사용)
    // 사용자의 artifact 수가 증가하면 메모리 이슈와 쿼리 성능 저하가 발생할 수 있습니다.
    // 가능하면 Page variant를 사용하는 것을 권장합니다.
    List<ProductionArtifactEntity> findTop1000ByUserIdAndCommandTypeOrderByCreatedAtDesc(Long userId, ProductionCommandType commandType);
    
    Page<ProductionArtifactEntity> findByUserIdAndCommandTypeOrderByCreatedAtDesc(Long userId, ProductionCommandType commandType, Pageable pageable);
    
    // ⚠️ 대량 데이터 로딩 위험: 최대 1000건만 반환 (Top1000 사용)
    // 사용자의 artifact 수가 증가하면 메모리 이슈와 쿼리 성능 저하가 발생할 수 있습니다.
    // 가능하면 Page variant를 사용하는 것을 권장합니다.
    List<ProductionArtifactEntity> findTop1000ByUserIdAndArtifactTypeOrderByCreatedAtDesc(Long userId, ArtifactType artifactType);
    
    Page<ProductionArtifactEntity> findByUserIdAndArtifactTypeOrderByCreatedAtDesc(Long userId, ArtifactType artifactType, Pageable pageable);
    
    long countByUserId(Long userId);
    
    long countByUserIdAndCommandType(Long userId, ProductionCommandType commandType);
    
    long countByUserIdAndArtifactType(Long userId, ArtifactType artifactType);
    
    // ⚠️ 인덱스 권장사항:
    // 데이터 증가 시 쿼리 성능 저하를 방지하기 위해 다음 인덱스 추가를 권장합니다:
    // - (userId, createdAt DESC) 복합 인덱스
    // - (userId, commandType, createdAt DESC) 복합 인덱스
    // - (userId, artifactType, createdAt DESC) 복합 인덱스
    // 엔티티 클래스에 @Index 어노테이션을 추가하거나 DDL에서 직접 생성하세요.
}

