package org.example.sharedprompts.module.domain.production.model.executor.email;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;

import java.util.UUID;

public record EmailCommand(
    @JsonIgnore String commandId,
    String subject,
    String recipient
) implements ProductionCommand {
    
    public EmailCommand(String subject, String recipient) {
        this(UUID.randomUUID().toString(), subject, recipient);
    }
    
    @Override
    public ProductionCommandType getCommandType() {
        return ProductionCommandType.EMAIL;
    }
    
    @Override
    public String getCommandId() {
        return commandId;
    }

    @Override
    public String getOutputFormat() {
        return "html";
    }
}

