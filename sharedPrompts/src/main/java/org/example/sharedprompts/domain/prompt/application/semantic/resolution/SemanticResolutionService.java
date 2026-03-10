package org.example.sharedprompts.domain.prompt.application.semantic.resolution;

import org.example.sharedprompts.domain.prompt.application.port.in.command.RecommendPromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.RecommendPromptResult;
import org.springframework.stereotype.Service;

/** 시맨틱 해석 파사드. 생성/추천 플로우 각각 위임 */
@Service
public class SemanticResolutionService {

    private final GenerationSemanticResolver generationSemanticResolver;
    private final RecommendationSemanticResolver recommendationSemanticResolver;

    public SemanticResolutionService(
            GenerationSemanticResolver generationSemanticResolver,
            RecommendationSemanticResolver recommendationSemanticResolver
    ) {
        this.generationSemanticResolver = generationSemanticResolver;
        this.recommendationSemanticResolver = recommendationSemanticResolver;
    }

    public ResolutionResult.Result resolve(UnifiedGeneratePromptCommand command) {
        return generationSemanticResolver.resolve(command);
    }

    public RecommendPromptResult resolveForRecommendation(RecommendPromptCommand command) {
        return recommendationSemanticResolver.resolveForRecommendation(command);
    }
}
