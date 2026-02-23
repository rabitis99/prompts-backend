package org.example.sharedprompts.module.domain.production.model.executor.literary;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.literary.LiteraryType;

import java.util.UUID;

public record LiteraryCommand(
        @JsonIgnore String commandId,
        LiteraryType literaryType
) implements ProductionCommand {

    public LiteraryCommand(LiteraryType literaryType) {
        this(UUID.randomUUID().toString(), literaryType);
    }

    @Override
    public ProductionCommandType getCommandType() {
        return ProductionCommandType.LITERARY;
    }

    @Override
    public String getCommandId() {
        return commandId;
    }

    @Override
    public String getOutputFormat() {
        return "literary";
    }
}
