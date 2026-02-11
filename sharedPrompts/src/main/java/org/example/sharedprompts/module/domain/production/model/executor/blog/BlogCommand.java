package org.example.sharedprompts.module.domain.production.model.executor.blog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;

import java.util.List;
import java.util.UUID;

public record BlogCommand(
    @JsonIgnore String commandId,
    String title,
    List<String> tags
) implements ProductionCommand {
    
    public BlogCommand(String title, List<String> tags) {
        this(UUID.randomUUID().toString(), title, tags);
    }
    
    @Override
    public ProductionCommandType getCommandType() {
        return ProductionCommandType.BLOG;
    }
    
    @Override
    public String getCommandId() {
        return commandId;
    }

    @Override
    public String getOutputFormat() {
        return "md";
    }
}

