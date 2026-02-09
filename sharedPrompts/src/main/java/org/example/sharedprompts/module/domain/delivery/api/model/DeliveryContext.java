package org.example.sharedprompts.module.domain.delivery.api.model;

import org.example.sharedprompts.module.domain.delivery.api.type.DeliveryType;

import java.util.HashMap;
import java.util.Map;

public class DeliveryContext {
    private final DeliveryType deliveryType;
    private final Long userId;
    private final Map<String, Object> attributes;
    
    public DeliveryContext(DeliveryType deliveryType, Long userId) {
        this.deliveryType = deliveryType;
        this.userId = userId;
        this.attributes = new HashMap<>();
    }
    
    public DeliveryType getDeliveryType() {
        return deliveryType;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, Class<T> type) {
        Object value = attributes.get(key);
        if (value == null) {
            return null;
        }
        if (!type.isInstance(value)) {
            throw new ClassCastException(
                String.format("Attribute '%s' is not of type %s, but %s", 
                    key, type.getName(), value.getClass().getName())
            );
        }
        return (T) value;
    }
    
    // BlogDeliveryService에서 사용하는 platform getter
    public String getPlatform() {
        return getAttribute("platform", String.class);
    }
}

