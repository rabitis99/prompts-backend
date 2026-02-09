package org.example.sharedprompts.module.domain.production.api;

public interface ProductionCommand {
    ProductionCommandType getCommandType();
    String getCommandId();
}

