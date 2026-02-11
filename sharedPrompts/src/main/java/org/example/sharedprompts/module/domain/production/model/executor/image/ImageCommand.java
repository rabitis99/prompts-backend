package org.example.sharedprompts.module.domain.production.model.executor.image;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;

import java.util.UUID;

public record ImageCommand(
    @JsonIgnore String commandId,
    String prompt,
    int width,
    int height
) implements ProductionCommand {
    
    public ImageCommand(String prompt, int width, int height) {
        this(UUID.randomUUID().toString(), prompt, width, height);
    }
    
    @Override
    public ProductionCommandType getCommandType() {
        return ProductionCommandType.IMAGE;
    }
    
    @Override
    public String getCommandId() {
        return commandId;
    }
}

