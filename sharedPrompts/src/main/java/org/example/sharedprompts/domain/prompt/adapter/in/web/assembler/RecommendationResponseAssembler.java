package org.example.sharedprompts.domain.prompt.adapter.in.web.assembler;

import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.PromptRecommendationResponse;
import org.example.sharedprompts.domain.prompt.application.port.in.query.RecommendPromptResult;

/**
 * Assembles internal recommendation result into UX-aligned external API response.
 * Keeps policy/service layer independent from API contract.
 * Optional explanation: when true, action/role items get reason/source from trace (same source, multiple projections).
 */
public interface RecommendationResponseAssembler {

    /**
     * Convert application-layer result to external response DTO.
     * Category → Intent → Action → Role structure with stable keys.
     */
    PromptRecommendationResponse toResponse(RecommendPromptResult result);

    /**
     * Same as {@link #toResponse(RecommendPromptResult)} with optional per-item explanation from trace.
     * When includeExplanation is true and result has trace, action/role reason/source/orderingBasis are filled.
     */
    default PromptRecommendationResponse toResponse(RecommendPromptResult result, boolean includeExplanation) {
        return toResponse(result);
    }
}
