package org.example.sharedprompts.module.domain.production.module.blog;

import org.example.sharedprompts.module.domain.production.api.ProductionCommand;
import org.example.sharedprompts.module.domain.production.api.ProductionCommandType;

import java.util.List;

public class BlogCommand implements ProductionCommand {
    private final String commandId;
    private final String title;
    private final List<String> tags;
    
    public BlogCommand(String commandId, String title, List<String> tags) {
        this.commandId = commandId;
        this.title = title;
        this.tags = List.copyOf(tags);
    }
    
    @Override
    public ProductionCommandType getCommandType() {
        return ProductionCommandType.BLOG;
    }
    
    @Override
    public String getCommandId() {
        return commandId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public List<String> getTags() {
        return tags;
    }
}

