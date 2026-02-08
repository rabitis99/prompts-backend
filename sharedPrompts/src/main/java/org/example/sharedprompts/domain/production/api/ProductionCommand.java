package org.example.sharedprompts.domain.production.api;

public interface ProductionCommand {
    ProductionCommandType getCommandType();
    String getCommandId();
}

