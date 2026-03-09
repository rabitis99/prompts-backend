package org.example.sharedprompts.domain.prompt.adapter.in.web.mapper;

import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.RecommendPromptResponse;
import org.example.sharedprompts.domain.prompt.application.port.in.query.RecommendPromptResult;
import org.springframework.stereotype.Component;

@Component
public class RecommendPromptResponseMapper {

    public RecommendPromptResponse toResponse(RecommendPromptResult result) {
        return new RecommendPromptResponse(
                result.requestMode(),
                result.category(),
                result.recommendedIntent(),
                result.intentCandidates(),
                result.recommendedRole(),
                result.roleCandidates(),
                result.recommendedAction(),
                result.actionCandidates(),
                result.recommendedTone(),
                result.recommendedStyle(),
                result.axisSources(),
                result.recommendationHints(),
                result.validationWarnings(),
                result.fallbackApplied(),
                result.defaultSelection()
        );
    }
}
