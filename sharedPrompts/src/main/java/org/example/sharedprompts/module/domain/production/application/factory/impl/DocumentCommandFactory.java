package org.example.sharedprompts.module.domain.production.application.factory.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.application.factory.ProductionCommandFactory;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.executor.document.DocumentCommand;
import org.example.sharedprompts.module.dto.request.production.DocumentProductionRequestDto;
import org.example.sharedprompts.module.dto.request.production.ProductionRequest;
import org.springframework.stereotype.Component;

/**
 * DocumentCommand 생성 Factory
 */
@Component
@Slf4j
public class DocumentCommandFactory implements ProductionCommandFactory {
    
    @Override
    public ProductionCommandType getSupportedCommandType() {
        return ProductionCommandType.DOCUMENT;
    }
    
    @Override
    public Class<? extends ProductionRequest> getSupportedRequestType() {
        return DocumentProductionRequestDto.class;
    }
    
    @Override
    public ProductionCommand createCommand(ProductionRequest request) {
        if (!(request instanceof DocumentProductionRequestDto documentRequest)) {
            throw new IllegalArgumentException(
                    "Expected DocumentProductionRequestDto, but got: " + 
                    (request != null ? request.getClass().getName() : "null"));
        }
        
        log.debug("Creating DocumentCommand from request - fileName: {}, format: {}", 
                documentRequest.fileName(), documentRequest.format());
        
        return new DocumentCommand(
                documentRequest.fileName(),
                documentRequest.format()
        );
    }
}

