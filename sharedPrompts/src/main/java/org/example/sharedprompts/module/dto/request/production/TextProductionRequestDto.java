package org.example.sharedprompts.module.dto.request.production;

import jakarta.validation.constraints.NotBlank;

public record TextProductionRequestDto(
    @NotBlank String fileName,
    @NotBlank String format,
    String userInput
) implements ProductionRequest {}

