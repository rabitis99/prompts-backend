package org.example.sharedprompts.module.domain.production.module.document;

import org.example.sharedprompts.module.domain.production.api.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.api.command.ProductionCommandType;

public class DocumentCommand implements ProductionCommand {
    private final String commandId;
    private final String format;
    private final String fileName;
    
    public DocumentCommand(String commandId, String format, String fileName) {
        this.commandId = commandId;
        this.format = format;
        this.fileName = fileName;
    }
    
    @Override
    public ProductionCommandType getCommandType() {
        return ProductionCommandType.DOCUMENT;
    }
    
    @Override
    public String getCommandId() {
        return commandId;
    }
    
    public String getFormat() {
        return format;
    }
    
    public String getFileName() {
        return fileName;
    }
}

