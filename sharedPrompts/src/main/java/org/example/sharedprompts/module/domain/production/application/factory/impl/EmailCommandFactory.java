package org.example.sharedprompts.module.domain.production.application.factory.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.application.factory.ProductionCommandFactory;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.executor.email.EmailCommand;
import org.example.sharedprompts.module.dto.request.production.EmailProductionRequestDto;
import org.example.sharedprompts.module.dto.request.production.ProductionRequest;
import org.springframework.stereotype.Component;

/**
 * EmailCommand 생성 Factory
 */
@Component
@Slf4j
public class EmailCommandFactory implements ProductionCommandFactory {
    
    @Override
    public ProductionCommandType getSupportedCommandType() {
        return ProductionCommandType.EMAIL;
    }
    
    @Override
    public Class<? extends ProductionRequest> getSupportedRequestType() {
        return EmailProductionRequestDto.class;
    }
    
    @Override
    public ProductionCommand createCommand(ProductionRequest request) {
        if (!(request instanceof EmailProductionRequestDto emailRequest)) {
            throw new IllegalArgumentException(
                    "Expected EmailProductionRequestDto, but got: " + 
                    (request != null ? request.getClass().getName() : "null"));
        }
        
        log.debug("Creating EmailCommand from request - subject: {}, recipient: {}", 
                emailRequest.subject(), emailRequest.recipient());
        
        return new EmailCommand(
                emailRequest.subject(),
                emailRequest.recipient()
        );
    }
}

