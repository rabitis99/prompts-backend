package org.example.sharedprompts.module.domain.production.application.factory.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.application.factory.ProductionCommandFactory;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.executor.blog.BlogCommand;
import org.example.sharedprompts.module.dto.request.production.BlogProductionRequestDto;
import org.example.sharedprompts.module.dto.request.production.ProductionRequest;
import org.springframework.stereotype.Component;

/**
 * BlogCommand 생성 Factory
 */
@Component
@Slf4j
public class BlogCommandFactory implements ProductionCommandFactory {
    
    @Override
    public ProductionCommandType getSupportedCommandType() {
        return ProductionCommandType.BLOG;
    }
    
    @Override
    public Class<? extends ProductionRequest> getSupportedRequestType() {
        return BlogProductionRequestDto.class;
    }
    
    @Override
    public ProductionCommand createCommand(ProductionRequest request) {
        if (!(request instanceof BlogProductionRequestDto blogRequest)) {
            throw new IllegalArgumentException(
                    "Expected BlogProductionRequestDto, but got: " + 
                    (request != null ? request.getClass().getName() : "null"));
        }
        
        log.debug("Creating BlogCommand from request - title: {}, tags: {}", 
                blogRequest.title(), blogRequest.tags());
        
        return new BlogCommand(
                blogRequest.title(),
                blogRequest.tags()
        );
    }
}

