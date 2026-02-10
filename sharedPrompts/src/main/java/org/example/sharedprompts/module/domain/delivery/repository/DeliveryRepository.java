package org.example.sharedprompts.module.domain.delivery.repository;

import org.example.sharedprompts.module.domain.delivery.api.type.DeliveryType;
import org.example.sharedprompts.module.domain.delivery.entity.DeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryRepository 
        extends JpaRepository<DeliveryEntity, Long>, CustomDeliveryRepository {
    
    Optional<DeliveryEntity> findFirstByProductionArtifactIdOrderByCreatedAtDesc(Long productionArtifactId);
    
    long countByUserId(Long userId);
    
    long countByUserIdAndDeliveryType(Long userId, DeliveryType deliveryType);
}

