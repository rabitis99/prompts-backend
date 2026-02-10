package org.example.sharedprompts.module.domain.delivery.repository;

import org.example.sharedprompts.module.domain.delivery.api.type.DeliveryType;
import org.example.sharedprompts.module.domain.delivery.entity.DeliveryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomDeliveryRepository {
    Page<DeliveryEntity> findPageByUserId(Long userId, Pageable pageable);
    Page<DeliveryEntity> findPageByUserIdAndDeliveryType(Long userId, DeliveryType deliveryType, Pageable pageable);
}

