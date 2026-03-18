package org.example.sharedprompts.domain.prompt.adapter.in.web.facade;

import org.example.sharedprompts.domain.prompt.adapter.in.web.assembler.RecommendationResponseAssembler;
import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.PromptRecommendationResponse;
import org.example.sharedprompts.domain.prompt.application.port.in.command.RecommendPromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.RecommendPromptResult;
import org.example.sharedprompts.domain.prompt.application.port.in.recommend.RecommendPromptAxesUseCase;
import org.example.sharedprompts.domain.prompt.application.semantic.audit.RecommendationAuditPublisher;
import org.example.sharedprompts.domain.prompt.application.semantic.observability.RecommendationObservabilityPublisher;
import org.example.sharedprompts.domain.prompt.common.enums.request.RequestMode;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.style.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.style.ToneType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Facade only orchestrates use case + assembler; no policy logic.
 */
class PromptRecommendationFacadeTest {

    @Test
    void recommend_callsUseCaseThenAssemblerAndPublishesObservability_returnsAssembledResponse() {
        RecommendPromptAxesUseCase useCase = mock(RecommendPromptAxesUseCase.class);
        RecommendationResponseAssembler assembler = mock(RecommendationResponseAssembler.class);
        RecommendationObservabilityPublisher observabilityPublisher = mock(RecommendationObservabilityPublisher.class);
        RecommendationAuditPublisher auditPublisher = mock(RecommendationAuditPublisher.class);

        RecommendPromptCommand command = new RecommendPromptCommand(
                RequestMode.ADVANCED, PromptCategory.WRITING, ActionIntent.GENERATE,
                null, null, ToneType.NEUTRAL, StyleType.NARRATIVE, null, null, null, null, null
        );
        RecommendPromptResult internalResult = new RecommendPromptResult(
                RequestMode.ADVANCED, PromptCategory.WRITING, ActionIntent.GENERATE,
                List.of(), null, List.of(), null, List.of(),
                ToneType.NEUTRAL, StyleType.NARRATIVE, null, List.of(), List.of(), List.of(), null
        );
        PromptRecommendationResponse assembled = new PromptRecommendationResponse(
                new PromptRecommendationResponse.CategoryIntentBlock("PROMPT_CATEGORY.WRITING", "글쓰기"),
                new PromptRecommendationResponse.CategoryIntentBlock("GENERATE", "Generate"),
                List.of(),
                new PromptRecommendationResponse.RecommendationMetadata(List.of(), List.of(), List.of(), "ADVANCED")
        );

        when(useCase.recommend(any(RecommendPromptCommand.class))).thenReturn(internalResult);
        when(assembler.toResponse(any(RecommendPromptResult.class))).thenReturn(assembled);

        PromptRecommendationFacade facade = new PromptRecommendationFacade(useCase, assembler, observabilityPublisher, auditPublisher);
        PromptRecommendationResponse response = facade.recommend(command);

        assertThat(response).isSameAs(assembled);
        verify(useCase).recommend(command);
        verify(assembler).toResponse(internalResult);
        verify(observabilityPublisher).publish(internalResult);
        verify(auditPublisher).publish(internalResult);
    }
}
