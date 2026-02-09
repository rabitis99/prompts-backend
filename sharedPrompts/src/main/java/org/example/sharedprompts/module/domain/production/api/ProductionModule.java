package org.example.sharedprompts.module.domain.production.api;

import org.example.sharedprompts.module.domain.production.exception.ProductionException;

public interface ProductionModule {
    ProductionCommandType getSupportedCommandType();
    ProductionResult produce(ProductionCommand command, ProductionContext context) 
            throws ProductionException;
}

