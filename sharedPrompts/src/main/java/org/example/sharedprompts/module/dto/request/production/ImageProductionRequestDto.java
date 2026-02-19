package org.example.sharedprompts.module.dto.request.production;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ImageProductionRequestDto(
    @NotNull @Min(1) Integer width,
    @NotNull @Min(1) Integer height,
    String userInput
) implements ProductionRequest {}

