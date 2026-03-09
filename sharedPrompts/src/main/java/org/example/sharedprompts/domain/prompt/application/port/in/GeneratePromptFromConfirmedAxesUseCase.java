package org.example.sharedprompts.domain.prompt.application.port.in;

import org.example.sharedprompts.domain.prompt.application.port.in.command.ConfirmedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.UnifiedGeneratePromptResult;

/**
 * Port for generating a prompt from already-confirmed semantic axes (POST /prompts/generate/confirmed).
 */
public interface GeneratePromptFromConfirmedAxesUseCase {

    UnifiedGeneratePromptResult generate(ConfirmedGeneratePromptCommand command);
}
