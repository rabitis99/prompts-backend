package org.example.sharedprompts.module.domain.production.service.prompt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.service.PromptService;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptTemplateService {

    private final PromptService promptService;
    private final PromptMerger promptMerger;
    private final PromptValidator promptValidator;

    @Transactional(readOnly = true)
    public MergedPrompt mergePrompt(Long promptId, Long userId, ProductionCommandType commandType, String userInput) {
        log.info("Merging prompt - promptId: {}, userId: {}, commandType: {}", promptId, userId, commandType);

        PromptResponseDto promptResult = promptService.getPromptDetail(promptId, userId);
        String promptContent = promptResult.getContent();
        //적용하지 않을 예정
        String promptVersion = "default";

        String mergedContent = promptMerger.merge(promptContent, userInput, commandType);

        promptValidator.validateVariables(mergedContent, commandType);
        promptValidator.validateLength(mergedContent);

        log.info("Prompt merged successfully - promptId: {}, version: {}, length: {}", promptId, promptVersion, mergedContent.length());

        return new MergedPrompt(mergedContent, promptVersion);
    }
}
