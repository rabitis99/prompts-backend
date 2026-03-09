package org.example.sharedprompts.domain.prompt.application.service.semantic;

import org.example.sharedprompts.domain.prompt.application.port.in.RecommendPromptAxesUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.command.RecommendPromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.RecommendPromptResult;
import org.springframework.stereotype.Service;

/**
 * Recommendation-only flow: resolves semantics and returns recommended axes without building
 * ConfirmedSemanticAxes or calling GeneratePromptUseCase.
 */
@Service
public class RecommendPromptAxesService implements RecommendPromptAxesUseCase {

    private final SemanticResolutionService semanticResolutionService;

    public RecommendPromptAxesService(SemanticResolutionService semanticResolutionService) {
        this.semanticResolutionService = semanticResolutionService;
    }

    @Override
    public RecommendPromptResult recommend(RecommendPromptCommand command) {
        return semanticResolutionService.resolveForRecommendation(command);
    }
}
