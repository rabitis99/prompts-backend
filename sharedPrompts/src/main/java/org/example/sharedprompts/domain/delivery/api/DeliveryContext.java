package org.example.sharedprompts.domain.delivery.api;

import java.util.Map;

public class DeliveryContext {
    private final DeliveryType deliveryType;
    private final Long userId;
    private final Map<String, Object> attributes;
    
    public DeliveryContext(DeliveryType deliveryType, Long userId) {
        this.deliveryType = deliveryType;
        this.userId = userId;
        this.attributes = new java.util.HashMap<>();
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
        return (T) attributes.get(key);
    }
    
    // BlogDeliveryService에서 사용하는 platform getter
    public String getPlatform() {
        return getAttribute("platform", String.class);
    }
}

