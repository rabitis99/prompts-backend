package org.example.sharedprompts.domain.prompt.adapter.in.web.mapper;

import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.RecommendPromptResponse;
import org.example.sharedprompts.domain.prompt.application.port.in.query.RecommendPromptResult;
import org.springframework.stereotype.Component;

/**
 * 추천 결과 → 응답 DTO 변환 매퍼
 */
@Component
public class RecommendPromptResponseMapper {

    /**
     * 추천 결과를 API 응답 DTO로 변환
     */
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