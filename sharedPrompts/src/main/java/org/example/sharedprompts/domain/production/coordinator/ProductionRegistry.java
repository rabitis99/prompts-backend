package org.example.sharedprompts.domain.production.coordinator;

import org.example.sharedprompts.domain.production.api.ProductionCommandType;
import org.example.sharedprompts.domain.production.api.ProductionModule;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProductionRegistry {
    
    private final Map<ProductionCommandType, ProductionModule> modules = new ConcurrentHashMap<>();
    
    public void register(ProductionModule module) {
        modules.put(module.getSupportedCommandType(), module);
    }
    
    public ProductionModule findModule(ProductionCommandType commandType) {
        return modules.get(commandType);
    }
}

