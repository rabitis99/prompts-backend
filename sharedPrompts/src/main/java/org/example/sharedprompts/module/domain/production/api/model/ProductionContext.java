package org.example.sharedprompts.module.domain.production.api.model;

import java.util.HashMap;
import java.util.Map;

public class ProductionContext {
    private final Long userId;
    private final Map<String, Object> attributes;
    
    public ProductionContext(Long userId) {
        this.userId = userId;
        this.attributes = new HashMap<>();
    }
    
    public Long getUserId() { return userId; }
    
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
}

