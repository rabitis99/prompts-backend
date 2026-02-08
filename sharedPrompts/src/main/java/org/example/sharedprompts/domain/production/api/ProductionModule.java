package org.example.sharedprompts.domain.production.api;

import org.example.sharedprompts.domain.production.exception.ProductionException;

public interface ProductionModule {
    ProductionCommandType getSupportedCommandType();
    ProductionResult produce(ProductionCommand command, ProductionContext context) 
            throws ProductionException;
}

