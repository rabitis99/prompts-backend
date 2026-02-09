package org.example.sharedprompts.module.domain.delivery.repository;

import org.example.sharedprompts.module.domain.delivery.api.type.DeliveryType;
import org.example.sharedprompts.module.domain.delivery.entity.DeliveryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<DeliveryEntity, Long> {
    
    Optional<DeliveryEntity> findByDeliveryId(String deliveryId);
    
    Optional<DeliveryEntity> findFirstByProductionArtifactIdOrderByCreatedAtDesc(Long productionArtifactId);
    
    // ⚠️ 대량 데이터 로딩 위험: 최대 1000건만 반환 (Top1000 사용)
    // 사용자의 delivery 수가 증가하면 메모리 이슈와 쿼리 성능 저하가 발생할 수 있습니다.
    // 가능하면 Page variant를 사용하는 것을 권장합니다.
    List<DeliveryEntity> findTop1000ByUserIdOrderByCreatedAtDesc(Long userId);
    
    Page<DeliveryEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    // ⚠️ 대량 데이터 로딩 위험: 최대 1000건만 반환 (Top1000 사용)
    // 사용자의 delivery 수가 증가하면 메모리 이슈와 쿼리 성능 저하가 발생할 수 있습니다.
    // 가능하면 Page variant를 사용하는 것을 권장합니다.
    List<DeliveryEntity> findTop1000ByUserIdAndDeliveryTypeOrderByCreatedAtDesc(Long userId, DeliveryType deliveryType);
    
    Page<DeliveryEntity> findByUserIdAndDeliveryTypeOrderByCreatedAtDesc(Long userId, DeliveryType deliveryType, Pageable pageable);
    
    long countByUserId(Long userId);
    
    long countByUserIdAndDeliveryType(Long userId, DeliveryType deliveryType);
    
    // ✅ 인덱스 적용됨:
    // DeliveryEntity에 다음 복합 인덱스가 적용되어 있습니다:
    // - (userId, createdAt) - findByUserIdOrderByCreatedAtDesc 쿼리 최적화
    // - (userId, deliveryType, createdAt) - findByUserIdAndDeliveryTypeOrderByCreatedAtDesc 쿼리 최적화
    // - (productionArtifactId) - findFirstByProductionArtifactIdOrderByCreatedAtDesc 쿼리 최적화
    // - (deliveryId) - findByDeliveryId 쿼리 최적화
}

