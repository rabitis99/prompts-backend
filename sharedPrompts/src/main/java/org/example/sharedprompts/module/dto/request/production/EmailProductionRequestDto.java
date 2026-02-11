package org.example.sharedprompts.module.dto.request.production;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record EmailProductionRequestDto(
    @NotNull String subject,
    @NotNull @Email String recipient,
    String userInput
) implements ProductionRequest {}

