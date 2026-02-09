package org.example.sharedprompts.domain.production.module.email;

import org.example.sharedprompts.domain.production.api.ProductionCommand;
import org.example.sharedprompts.domain.production.api.ProductionCommandType;

public class EmailCommand implements ProductionCommand {
    private final String commandId;
    private final String subject;
    private final String recipient;
    
    public EmailCommand(String commandId, String subject, String recipient) {
        this.commandId = commandId;
        this.subject = subject;
        this.recipient = recipient;
    }
    
    @Override
    public ProductionCommandType getCommandType() {
        return ProductionCommandType.EMAIL;
    }
    
    @Override
    public String getCommandId() {
        return commandId;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public String getRecipient() {
        return recipient;
    }
}

