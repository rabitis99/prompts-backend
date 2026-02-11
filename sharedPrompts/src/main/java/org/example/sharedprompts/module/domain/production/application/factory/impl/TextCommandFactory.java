package org.example.sharedprompts.module.domain.production.application.factory.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.application.factory.ProductionCommandFactory;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.executor.text.TextCommand;
import org.example.sharedprompts.module.dto.request.production.ProductionRequest;
import org.example.sharedprompts.module.dto.request.production.TextProductionRequestDto;
import org.springframework.stereotype.Component;

/**
 * TextCommand 생성 Factory
 */
@Component
@Slf4j
public class TextCommandFactory implements ProductionCommandFactory {
    
    @Override
    public ProductionCommandType getSupportedCommandType() {
        return ProductionCommandType.TEXT;
    }
    
    @Override
    public Class<? extends ProductionRequest> getSupportedRequestType() {
        return TextProductionRequestDto.class;
    }
    
    @Override
    public ProductionCommand createCommand(ProductionRequest request) {
        if (!(request instanceof TextProductionRequestDto textRequest)) {
            throw new IllegalArgumentException(
                    "Expected TextProductionRequestDto, but got: " + 
                    (request != null ? request.getClass().getName() : "null"));
        }
        
        log.debug("Creating TextCommand from request - fileName: {}, format: {}", 
                textRequest.fileName(), textRequest.format());
        
        return new TextCommand(
                textRequest.fileName(),
                textRequest.format()
        );
    }
}

