package org.example.sharedprompts.module.domain.production.application.factory.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.application.factory.ProductionCommandFactory;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.executor.literary.LiteraryCommand;
import org.example.sharedprompts.module.dto.request.production.LiteraryProductionRequestDto;
import org.example.sharedprompts.module.dto.request.production.ProductionRequest;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LiteraryCommandFactory implements ProductionCommandFactory {

    @Override
    public ProductionCommandType getSupportedCommandType() {
        return ProductionCommandType.LITERARY;
    }

    @Override
    public Class<? extends ProductionRequest> getSupportedRequestType() {
        return LiteraryProductionRequestDto.class;
    }

    @Override
    public ProductionCommand createCommand(ProductionRequest request) {
        if (!(request instanceof LiteraryProductionRequestDto literaryRequest)) {
            throw new IllegalArgumentException(
                    "Expected LiteraryProductionRequestDto, but got: " +
                            (request != null ? request.getClass().getName() : "null"));
        }
        log.debug("Creating LiteraryCommand from request - literaryType: {}", literaryRequest.literaryType());
        return new LiteraryCommand(literaryRequest.literaryType());
    }
}
