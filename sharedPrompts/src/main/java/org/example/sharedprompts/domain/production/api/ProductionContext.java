package org.example.sharedprompts.domain.production.api;

import java.util.Map;
import java.util.UUID;

public class ProductionContext {
    private final String productionId;
    private final Long userId;
    private final Map<String, Object> attributes;
    
    public ProductionContext(Long userId) {
        this.productionId = UUID.randomUUID().toString();
        this.userId = userId;
        this.attributes = new java.util.HashMap<>();
    }
    
    public String getProductionId() { return productionId; }
    public Long getUserId() { return userId; }
    
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, Class<T> type) {
        return (T) attributes.get(key);
    }
}

