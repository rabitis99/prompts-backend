package org.example.sharedprompts.module.domain.production.model.executor.literary;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.literary.LiteraryType;

import java.util.UUID;

/**
 * Literary production command. The {@code commandId} is a one-time identifier regenerated
 * on each deserialization (e.g. for request correlation); do not use it for persistence
 * or idempotency—use job-level idempotency keys instead.
 */
public record LiteraryCommand(
        @JsonIgnore String commandId,
        LiteraryType literaryType
) implements ProductionCommand {

    @JsonCreator
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
