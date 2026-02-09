package org.example.sharedprompts.module.domain.production.module.image;

import org.example.sharedprompts.module.domain.production.api.ProductionCommand;
import org.example.sharedprompts.module.domain.production.api.ProductionCommandType;

public class ImageCommand implements ProductionCommand {
    private final String commandId;
    private final String prompt;
    private final int width;
    private final int height;
    
    public ImageCommand(String commandId, String prompt, int width, int height) {
        this.commandId = commandId;
        this.prompt = prompt;
        this.width = width;
        this.height = height;
    }
    
    @Override
    public ProductionCommandType getCommandType() {
        return ProductionCommandType.IMAGE;
    }
    
    @Override
    public String getCommandId() {
        return commandId;
    }
    
    public String getPrompt() {
        return prompt;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
}

