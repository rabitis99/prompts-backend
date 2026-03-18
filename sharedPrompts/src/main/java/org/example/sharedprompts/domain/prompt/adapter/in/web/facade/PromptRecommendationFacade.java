package org.example.sharedprompts.domain.prompt.adapter.in.web.facade;

import org.example.sharedprompts.domain.prompt.adapter.in.web.assembler.RecommendationResponseAssembler;
import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.PromptRecommendationResponse;
import org.example.sharedprompts.domain.prompt.application.port.in.command.RecommendPromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.RecommendPromptResult;
import org.example.sharedprompts.domain.prompt.application.port.in.recommend.RecommendPromptAxesUseCase;
import org.example.sharedprompts.domain.prompt.application.semantic.audit.RecommendationAuditPublisher;
import org.example.sharedprompts.domain.prompt.application.semantic.observability.RecommendationObservabilityPublisher;
import org.springframework.stereotype.Component;

/**
 * Facade for recommendation API: orchestration + response assembly + observability + audit.
 * Controller delegates here; no policy logic in controller.
 * Publishes observability event and audit record after each recommendation.
 */
@Component
public class PromptRecommendationFacade {

    private final RecommendPromptAxesUseCase recommendPromptAxesUseCase;
    private final RecommendationResponseAssembler responseAssembler;
    private final RecommendationObservabilityPublisher observabilityPublisher;
    private final RecommendationAuditPublisher auditPublisher;

    public PromptRecommendationFacade(
            RecommendPromptAxesUseCase recommendPromptAxesUseCase,
            RecommendationResponseAssembler responseAssembler,
            RecommendationObservabilityPublisher observabilityPublisher,
            RecommendationAuditPublisher auditPublisher
    ) {
        this.recommendPromptAxesUseCase = recommendPromptAxesUseCase;
        this.responseAssembler = responseAssembler;
        this.observabilityPublisher = observabilityPublisher;
        this.auditPublisher = auditPublisher;
    }

    /**
     * Run recommendation and return UX-aligned response (Category → Intent → Action → Role).
     * Publishes observability and audit for the result.
     */
    public PromptRecommendationResponse recommend(RecommendPromptCommand command) {
        RecommendPromptResult result = recommendPromptAxesUseCase.recommend(command);
        observabilityPublisher.publish(result);
        auditPublisher.publish(result);
        return responseAssembler.toResponse(result);
    }
}
