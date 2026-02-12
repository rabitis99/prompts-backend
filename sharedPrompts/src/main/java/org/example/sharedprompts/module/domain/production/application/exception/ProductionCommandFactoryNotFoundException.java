package org.example.sharedprompts.module.domain.production.application.exception;

import static org.example.sharedprompts.module.exception.ModuleErrorCode.COMMAND_FACTORY_NOT_FOUND;

public class ProductionCommandFactoryNotFoundException extends ProductionApplicationException {
    
    public ProductionCommandFactoryNotFoundException(String message) {
        super(COMMAND_FACTORY_NOT_FOUND, message);
    }
    
    public ProductionCommandFactoryNotFoundException(Class<?> requestType) {
        super(COMMAND_FACTORY_NOT_FOUND, 
                "ProductionCommandFactory not found for request type: " + requestType.getName());
    }
}





