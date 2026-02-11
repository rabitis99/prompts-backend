package org.example.sharedprompts.module.domain.production.model.executor.document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;

import java.util.UUID;

public record DocumentCommand(
    @JsonIgnore String commandId,
    String fileName,
    String format
) implements ProductionCommand {
    
    public DocumentCommand(String fileName, String format) {
        this(UUID.randomUUID().toString(), fileName, format);
    }
    
    @Override
    public ProductionCommandType getCommandType() {
        return ProductionCommandType.DOCUMENT;
    }
    
    @Override
    public String getCommandId() {
        return commandId;
    }

    @Override
    public String getOutputFormat() {
        return format != null ? format.toLowerCase() : "md";
    }
}

