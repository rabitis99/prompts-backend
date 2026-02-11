package org.example.sharedprompts.module.domain.production.model.executor.text;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;

import java.util.UUID;

public record TextCommand(
    @JsonIgnore String commandId,
    String fileName,
    String format
) implements ProductionCommand {
    
    public TextCommand(String fileName, String format) {
        this(UUID.randomUUID().toString(), fileName, format);
    }
    
    @Override
    public ProductionCommandType getCommandType() {
        return ProductionCommandType.TEXT;
    }
    
    @Override
    public String getCommandId() {
        return commandId;
    }

    @Override
    public String getOutputFormat() {
        return format != null ? format.toLowerCase() : "txt";
    }
}

