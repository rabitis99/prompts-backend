package org.example.sharedprompts.module.domain.production.api.module;

import org.example.sharedprompts.module.domain.production.api.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.api.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.api.model.ProductionContext;
import org.example.sharedprompts.module.domain.production.api.model.ProductionResult;
import org.example.sharedprompts.module.domain.production.exception.ProductionException;

public interface ProductionModule {
    ProductionCommandType getSupportedCommandType();
    ProductionResult produce(ProductionCommand command, ProductionContext context) 
            throws ProductionException;
}

