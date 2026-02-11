package org.example.sharedprompts.module.domain.production.service.job.process.util;

import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.service.ai.ContentType;
import org.springframework.stereotype.Component;

@Component
public class ContentTypeDeterminer {

    public ContentType determine(ProductionCommand command) {
        ProductionCommandType commandType = command.getCommandType();
        return switch (commandType) {
            case TEXT, EMAIL, BLOG, DOCUMENT -> ContentType.TEXT;
            case IMAGE -> ContentType.IMAGE;
        };
    }
}

