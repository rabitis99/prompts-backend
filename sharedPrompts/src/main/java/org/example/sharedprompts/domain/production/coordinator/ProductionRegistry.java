package org.example.sharedprompts.domain.production.coordinator;

import org.example.sharedprompts.domain.production.api.ProductionCommandType;
import org.example.sharedprompts.domain.production.api.ProductionModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProductionRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(ProductionRegistry.class);
    
    private final Map<ProductionCommandType, ProductionModule> modules = new ConcurrentHashMap<>();
    
    public ProductionRegistry(List<ProductionModule> productionModules) {
        productionModules.forEach(this::register);
    }
    
    public void register(ProductionModule module) {
        ProductionModule existing = modules.putIfAbsent(module.getSupportedCommandType(), module);
        if (existing != null) {
            log.warn("Duplicate module registration for type: {}", module.getSupportedCommandType());
        }
    }
    
    public ProductionModule findModule(ProductionCommandType commandType) {
        return modules.get(commandType);
    }
}

