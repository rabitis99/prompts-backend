package org.example.sharedprompts.module.dto.request.production;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.sharedprompts.module.domain.production.model.literary.LiteraryFormat;
import org.example.sharedprompts.module.domain.production.model.literary.LiteraryType;

public record LiteraryProductionRequestDto(
        @NotNull LiteraryType literaryType,
        @NotBlank String fileName,
        @NotNull LiteraryFormat format,
        @Nullable String userInput
) implements ProductionRequest {}
