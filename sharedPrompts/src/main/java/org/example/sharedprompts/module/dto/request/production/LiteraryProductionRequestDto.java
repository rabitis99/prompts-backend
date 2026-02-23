package org.example.sharedprompts.module.dto.request.production;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.sharedprompts.module.domain.production.model.literary.LiteraryType;

public record LiteraryProductionRequestDto(
        @NotNull LiteraryType literaryType,
        @NotBlank String fileName,
        @NotBlank String format,
        String userInput
) implements ProductionRequest {}
