package org.example.sharedprompts.module.domain.production.service.prompt.literary;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.literary.LiteraryType;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class LiteraryPromptComposer {

    public String compose(String promptContent, String userInput, LiteraryType literaryType) {
        Objects.requireNonNull(literaryType, "literaryType must not be null");
        String formatRulesText = LiteraryFormatRules.getFormatRules(literaryType);
        String withUserInput = mergeUserInput(promptContent, userInput);
        String composed = withUserInput + "\n\n---\n\n" + formatRulesText;
        log.debug("Literary prompt composed - literaryType: {}, length: {}", literaryType, composed.length());
        return composed;
    }

    private String mergeUserInput(String promptContent, String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return promptContent != null ? promptContent : "";
        }
        if (promptContent != null && promptContent.contains("{{userInput}}")) {
            return promptContent.replace("{{userInput}}", userInput);
        }
        return (promptContent != null ? promptContent : "") + "\n\n사용자 요청: " + userInput;
    }
}
