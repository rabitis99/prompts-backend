package org.example.sharedprompts.domain.production.exception;

import org.example.sharedprompts.domain.production.api.ProductionCommandType;

public class ProductionModuleNotFoundException extends ProductionException {
    public ProductionModuleNotFoundException(ProductionCommandType commandType) {
        super("Production module not found for type: " + commandType);
    }
}

