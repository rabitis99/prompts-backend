package org.example.sharedprompts.module.domain.production.service.prompt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.application.port.in.query.PromptDetailView;
import org.example.sharedprompts.domain.prompt.application.port.in.prompt.PromptQueryUseCase;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.executor.literary.LiteraryCommand;
import org.example.sharedprompts.module.domain.production.service.prompt.literary.LiteraryPromptComposer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptTemplateService {

    private final PromptQueryUseCase promptQueryUseCase;
    private final PromptMerger promptMerger;
    private final PromptValidator promptValidator;
    private final LiteraryPromptComposer literaryPromptComposer;

    @Transactional(readOnly = true)
    public MergedPrompt mergePrompt(Long promptId, Long userId, ProductionCommand command, String userInput) {
        var commandType = command.getCommandType();
        log.info("Merging prompt - promptId: {}, userId: {}, commandType: {}", promptId, userId, commandType);

        PromptDetailView promptResult = promptQueryUseCase.getPromptDetail(promptId, userId);
        String promptContent = promptResult.content();
        String promptVersion = "default";

        String mergedContent;
        if (command instanceof LiteraryCommand literaryCommand) {
            mergedContent = literaryPromptComposer.compose(promptContent, userInput, literaryCommand.literaryType());
        } else {
            mergedContent = promptMerger.merge(promptContent, userInput, commandType);
        }

        promptValidator.validateVariables(mergedContent, commandType);
        promptValidator.validateLength(mergedContent);

        log.info("Prompt merged successfully - promptId: {}, version: {}, length: {}", promptId, promptVersion, mergedContent.length());
        return new MergedPrompt(mergedContent, promptVersion);
    }
}
