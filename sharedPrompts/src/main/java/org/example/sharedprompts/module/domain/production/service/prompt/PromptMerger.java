package org.example.sharedprompts.module.domain.production.service.prompt;

import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.springframework.stereotype.Component;

@Component
public class PromptMerger {

    public String merge(String promptContent, String userInput, ProductionCommandType commandType) {
        if (userInput == null || userInput.isBlank()) {
            return promptContent;
        }

        return switch (commandType) {
            case BLOG -> mergeBlogPrompt(promptContent, userInput);
            case EMAIL -> mergeEmailPrompt(promptContent, userInput);
            case DOCUMENT -> mergeDocumentPrompt(promptContent, userInput);
            default -> mergeDefaultPrompt(promptContent, userInput);
        };
    }

    private String mergeBlogPrompt(String promptContent, String userInput) {
        return replaceOrAppend(promptContent, userInput, "사용자 요청: ");
    }

    private String mergeEmailPrompt(String promptContent, String userInput) {
        return replaceOrAppend(promptContent, userInput, "이메일 내용 요청: ");
    }

    private String mergeDocumentPrompt(String promptContent, String userInput) {
        return replaceOrAppend(promptContent, userInput, "문서 내용 요청: ");
    }

    private String mergeDefaultPrompt(String promptContent, String userInput) {
        return replaceOrAppend(promptContent, userInput, "");
    }

    private String replaceOrAppend(String promptContent, String userInput, String suffix) {
        if (promptContent.contains("{{userInput}}")) {
            return promptContent.replace("{{userInput}}", userInput);
        }
        return promptContent + "\n\n" + suffix + userInput;
    }
}
