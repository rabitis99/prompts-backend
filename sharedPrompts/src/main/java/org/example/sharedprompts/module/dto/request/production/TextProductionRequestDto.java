package org.example.sharedprompts.module.dto.request.production;

public record TextProductionRequestDto(
    String userInput
) implements ProductionRequest {}

