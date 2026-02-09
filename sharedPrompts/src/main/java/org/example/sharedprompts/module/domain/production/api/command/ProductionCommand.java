package org.example.sharedprompts.module.domain.production.api.command;

public interface ProductionCommand {
    ProductionCommandType getCommandType();
    String getCommandId();
}

