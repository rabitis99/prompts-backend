package org.example.sharedprompts.module.domain.production.service.prompt;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PromptValidator {

    private static final int MAX_PROMPT_LENGTH = 10000;

    public void validateVariables(String mergedPrompt, ProductionCommandType commandType) {
        if (mergedPrompt.contains("{{") && mergedPrompt.contains("}}")) {
            log.warn("Unresolved placeholders may exist in prompt - commandType: {}", commandType);
        }
    }

    public void validateLength(String mergedPrompt) {
        if (mergedPrompt.length() > MAX_PROMPT_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Prompt length exceeds maximum: %d > %d", mergedPrompt.length(), MAX_PROMPT_LENGTH));
        }
    }
}
