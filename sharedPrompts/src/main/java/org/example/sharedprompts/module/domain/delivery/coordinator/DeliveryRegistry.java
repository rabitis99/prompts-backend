package org.example.sharedprompts.module.domain.delivery.coordinator;


import org.example.sharedprompts.module.domain.delivery.api.service.DeliveryService;
import org.example.sharedprompts.module.domain.delivery.api.type.DeliveryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DeliveryRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(DeliveryRegistry.class);
    
    private final Map<DeliveryType, DeliveryService> services = new ConcurrentHashMap<>();
    
    public DeliveryRegistry(List<DeliveryService> deliveryServices) {
        deliveryServices.forEach(this::register);
    }
    
    public void register(DeliveryService service) {
        DeliveryService existing = services.putIfAbsent(service.getSupportedDeliveryType(), service);
        if (existing != null) {
            log.warn("Duplicate service registration for type: {}", service.getSupportedDeliveryType());
        }
    }
    
    public DeliveryService find(DeliveryType deliveryType) {
        return services.get(deliveryType);
    }
}

