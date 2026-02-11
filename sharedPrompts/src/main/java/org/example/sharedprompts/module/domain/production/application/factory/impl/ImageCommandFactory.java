package org.example.sharedprompts.module.domain.production.application.factory.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.application.factory.ProductionCommandFactory;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.executor.image.ImageCommand;
import org.example.sharedprompts.module.dto.request.production.ImageProductionRequestDto;
import org.example.sharedprompts.module.dto.request.production.ProductionRequest;
import org.springframework.stereotype.Component;

/**
 * ImageCommand 생성 Factory
 */
@Component
@Slf4j
public class ImageCommandFactory implements ProductionCommandFactory {
    
    @Override
    public ProductionCommandType getSupportedCommandType() {
        return ProductionCommandType.IMAGE;
    }
    
    @Override
    public Class<? extends ProductionRequest> getSupportedRequestType() {
        return ImageProductionRequestDto.class;
    }
    
    @Override
    public ProductionCommand createCommand(ProductionRequest request) {
        if (!(request instanceof ImageProductionRequestDto imageRequest)) {
            throw new IllegalArgumentException(
                    "Expected ImageProductionRequestDto, but got: " + 
                    (request != null ? request.getClass().getName() : "null"));
        }
        
        log.debug("Creating ImageCommand from request - prompt: {}, size: {}x{}", 
                imageRequest.prompt(), imageRequest.width(), imageRequest.height());
        
        return new ImageCommand(
                imageRequest.prompt(),
                imageRequest.width(),
                imageRequest.height()
        );
    }
}

