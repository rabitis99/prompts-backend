package org.example.sharedprompts.domain.production.coordinator;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.production.api.ProductionCommand;
import org.example.sharedprompts.domain.production.api.ProductionContext;
import org.example.sharedprompts.domain.production.api.ProductionModule;
import org.example.sharedprompts.domain.production.api.ProductionResult;
import org.example.sharedprompts.domain.production.exception.ProductionModuleNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductionCoordinator {
    
    private final ProductionRegistry registry;
    
    public ProductionResult produce(
            ProductionCommand command, 
            ProductionContext context
    ) {
        ProductionModule module = registry.findModule(command.getCommandType());
        
        if (module == null) {
            throw new ProductionModuleNotFoundException(command.getCommandType());
        }
        
        return module.produce(command, context);
    }
}

