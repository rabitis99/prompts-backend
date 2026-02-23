package org.example.sharedprompts.module.domain.production.service.job.process.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.executor.blog.BlogCommand;
import org.example.sharedprompts.module.domain.production.model.executor.document.DocumentCommand;
import org.example.sharedprompts.module.domain.production.model.executor.email.EmailCommand;
import org.example.sharedprompts.module.domain.production.model.executor.image.ImageCommand;
import org.example.sharedprompts.module.domain.production.model.executor.literary.LiteraryCommand;
import org.example.sharedprompts.module.domain.production.model.executor.text.TextCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommandDeserializer {

    private final ObjectMapper objectMapper;

    public ProductionCommand deserialize(JobEntity job) {
        try {
            String commandType = job.getCommandType();
            Class<? extends ProductionCommand> commandClass = getCommandClass(commandType);
            return objectMapper.readValue(job.getCommandJson(), commandClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize command: " + e.getMessage(), e);
        }
    }

    private Class<? extends ProductionCommand> getCommandClass(String commandType) {
        if (commandType == null) {
            throw new IllegalArgumentException("Command type must not be null");
        }
        ProductionCommandType type;
        try {
            type = ProductionCommandType.valueOf(commandType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown command type: " + commandType, e);
        }
        return switch (type) {
            case TEXT -> TextCommand.class;
            case IMAGE -> ImageCommand.class;
            case EMAIL -> EmailCommand.class;
            case BLOG -> BlogCommand.class;
            case DOCUMENT -> DocumentCommand.class;
            case LITERARY -> LiteraryCommand.class;
        };
    }
}

