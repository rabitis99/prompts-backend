package org.example.sharedprompts.module.domain.production.module.text;

import org.example.sharedprompts.module.domain.production.api.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.api.command.ProductionCommandType;

public class TextCommand implements ProductionCommand {
    private final String commandId;
    private final String fileName;
    private final String format;
    
    public TextCommand(String commandId, String fileName, String format) {
        this.commandId = commandId;
        this.fileName = fileName;
        this.format = format;
    }
    
    @Override
    public ProductionCommandType getCommandType() {
        return ProductionCommandType.TEXT;
    }
    
    @Override
    public String getCommandId() {
        return commandId;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public String getFormat() {
        return format;
    }
}

