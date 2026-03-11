package org.example.sharedprompts.domain.prompt.application.semantic.recommendation;

import org.example.sharedprompts.domain.prompt.application.port.in.recommend.RecommendPromptAxesUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.command.RecommendPromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.RecommendPromptResult;
import org.example.sharedprompts.domain.prompt.application.semantic.resolution.SemanticResolutionService;
import org.springframework.stereotype.Service;

/** 추천 유즈케이스: 시맨틱 해석 후 추천 축만 반환 */
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
