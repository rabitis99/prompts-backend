package org.example.sharedprompts.module.domain.production.exception;

import org.example.sharedprompts.module.domain.production.api.command.ProductionCommandType;

public class ProductionModuleNotFoundException extends ProductionException {
    public ProductionModuleNotFoundException(ProductionCommandType commandType) {
        super("Production module not found for type: " + commandType);
    }
}

