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
        
        log.debug("Creating ImageCommand from request - size: {}x{}", 
                imageRequest.width(), imageRequest.height());
        
        // prompt는 나중에 PromptTemplateService에서 병합된 프롬프트로 설정됨
        // 여기서는 null로 설정하고, ImageCommandValidator는 나중에 검증
        return new ImageCommand(
                null, // prompt는 나중에 설정됨
                imageRequest.width(),
                imageRequest.height()
        );
    }
}

