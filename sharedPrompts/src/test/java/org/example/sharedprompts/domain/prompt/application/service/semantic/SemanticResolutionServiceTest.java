package org.example.sharedprompts.domain.prompt.application.service.semantic;

import org.example.sharedprompts.domain.prompt.application.port.in.command.RecommendPromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.RecommendPromptResult;
import org.example.sharedprompts.domain.prompt.common.AxisSourceConstants;
import org.example.sharedprompts.domain.prompt.common.enums.*;
import org.example.sharedprompts.domain.prompt.domain.semantic.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SemanticResolutionServiceTest {

    private CategorySemanticProfileRegistry profileRegistry;
    private SemanticRecommendationService recommendationService;
    private SemanticValidationService validationService;
    private SemanticResolutionService resolutionService;

    @BeforeEach
    void setUp() {
        profileRegistry = mock(CategorySemanticProfileRegistry.class);
        recommendationService = mock(SemanticRecommendationService.class);
        validationService = mock(SemanticValidationService.class);

        resolutionService = new SemanticResolutionService(
                profileRegistry, recommendationService, validationService
        );
    }

    @Test
    void quickGeneration_userProvidesAllAxes() {
        UnifiedGeneratePromptCommand command = UnifiedGeneratePromptCommand.of(
                1L, RequestMode.ADVANCED, PromptCategory.ETC, ActionIntent.GENERATE, null, "input", null,
                EngineMode.AUTO, ToneType.NEUTRAL, StyleType.NARRATIVE, LanguageType.KOREAN, ExperienceLevel.INTERMEDIATE,
                false, null, null, null, "title", "desc"
        );

        when(validationService.validate(any(), any(), any())).thenReturn(new SemanticValidationResult(SemanticValidationResult.Severity.INFO, List.of()));
        when(recommendationService.recommend(any(), any(), any(), any(), any(), eq(false)))
                .thenReturn(new SemanticRecommendationService.RecommendationResult(Optional.empty(), List.of(), Optional.empty(), List.of(), List.of()));

        SemanticResolutionService.Result result = resolutionService.resolve(command);

        assertThat(result.success()).isTrue();
        assertThat(result.axes().category()).isEqualTo(PromptCategory.ETC);
        assertThat(result.axes().intent()).isEqualTo(ActionIntent.GENERATE);
        assertThat(result.metadata().userProvidedIntent()).isTrue();
        assertThat(result.metadata().fallbackIntentUsed()).isFalse();
    }

    @Test
    void recommendationEndpoint_categoryOnlyInput_intentFallbackApplied() {
        RecommendPromptCommand command = new RecommendPromptCommand(
                RequestMode.ADVANCED, PromptCategory.ETC, null, null, null, ToneType.NEUTRAL, StyleType.NARRATIVE, LanguageType.KOREAN, ExperienceLevel.INTERMEDIATE, "input"
        );

        CategorySemanticProfile profile = mock(CategorySemanticProfile.class);
        when(profile.getFallbackIntent()).thenReturn(ActionIntent.GENERATE);
        when(profile.getAllowedIntents()).thenReturn(List.of(ActionIntent.GENERATE, ActionIntent.ANALYZE));
        when(profileRegistry.getProfile(PromptCategory.ETC)).thenReturn(Optional.of(profile));

        when(validationService.validate(any(), any(), any())).thenReturn(new SemanticValidationResult(SemanticValidationResult.Severity.INFO, List.of()));
        when(recommendationService.recommend(any(), any(), any(), any(), any(), eq(true)))
                .thenReturn(new SemanticRecommendationService.RecommendationResult(Optional.empty(), List.of(), Optional.empty(), List.of(), List.of()));

        RecommendPromptResult result = resolutionService.resolveForRecommendation(command);

        assertThat(result.resolvedIntent()).isEqualTo(ActionIntent.GENERATE);
        assertThat(result.fallbackAppliedWarnings()).contains("intent: profile fallback applied");
        assertThat(result.intentCandidates()).containsExactly(ActionIntent.GENERATE, ActionIntent.ANALYZE);
        assertThat(result.axisSources()).containsEntry("intent", AxisSourceConstants.FALLBACK);
    }
}
