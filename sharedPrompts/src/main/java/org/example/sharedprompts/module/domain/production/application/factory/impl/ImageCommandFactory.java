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
        
        // prompt는 JobProcessor에서 PromptTemplateService.mergePrompt()로 생성되어
        // AIJobExecutor에 별도 파라미터로 전달됩니다.
        // ImageCommand의 prompt 필드는 FileNameGenerator에서만 사용되며,
        // null일 경우 기본값("image-output")을 사용합니다.
        // 여기서는 빈 문자열로 초기화하여 validator를 통과시킵니다.
        return new ImageCommand(
                "", // prompt는 JobProcessor에서 별도로 생성되어 전달됨
                imageRequest.width(),
                imageRequest.height()
        );
    }
}

