package org.example.sharedprompts.domain.delivery.coordinator;

import org.example.sharedprompts.domain.delivery.api.DeliveryService;
import org.example.sharedprompts.domain.delivery.api.DeliveryType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DeliveryRegistry {
    
    private final Map<DeliveryType, DeliveryService> services = new ConcurrentHashMap<>();
    
    public void register(DeliveryService service) {
        services.put(service.getSupportedDeliveryType(), service);
    }
    
    public DeliveryService find(DeliveryType deliveryType) {
        return services.get(deliveryType);
    }
}

