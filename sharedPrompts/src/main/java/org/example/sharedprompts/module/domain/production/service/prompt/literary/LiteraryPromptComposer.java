package org.example.sharedprompts.module.domain.production.service.prompt.literary;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.literary.LiteraryType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LiteraryPromptComposer {

    private final LiteraryFormatRules formatRules;

    public String compose(String promptContent, String userInput, LiteraryType literaryType) {
        String formatRulesText = formatRules.getFormatRules(literaryType);
        String withUserInput = mergeUserInput(promptContent, userInput);
        String composed = withUserInput + "\n\n---\n\n" + formatRulesText;
        log.debug("Literary prompt composed - literaryType: {}, length: {}", literaryType, composed.length());
        return composed;
    }

    private String mergeUserInput(String promptContent, String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return promptContent;
        }
        if (promptContent != null && promptContent.contains("{{userInput}}")) {
            return promptContent.replace("{{userInput}}", userInput);
        }
        return (promptContent != null ? promptContent : "") + "\n\n사용자 요청: " + userInput;
    }
}
