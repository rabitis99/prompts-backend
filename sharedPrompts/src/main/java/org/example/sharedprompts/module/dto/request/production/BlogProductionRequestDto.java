package org.example.sharedprompts.module.dto.request.production;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BlogProductionRequestDto(
    @NotBlank String title,
    @NotNull List<String> tags,
    String userInput
) implements ProductionRequest {}

