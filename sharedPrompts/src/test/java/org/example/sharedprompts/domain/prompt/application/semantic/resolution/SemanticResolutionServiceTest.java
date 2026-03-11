package org.example.sharedprompts.domain.prompt.application.semantic.resolution;

import org.example.sharedprompts.domain.prompt.application.port.in.command.RecommendPromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.RecommendPromptResult;
import org.example.sharedprompts.domain.prompt.common.constants.AxisSourceConstants;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.domain.semantic.ConfirmedSemanticAxes;
import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SemanticResolutionServiceTest {

    private GenerationSemanticResolver generationSemanticResolver;
    private RecommendationSemanticResolver recommendationSemanticResolver;
    private SemanticResolutionService resolutionService;

    @BeforeEach
    void setUp() {
        generationSemanticResolver = mock(GenerationSemanticResolver.class);
        recommendationSemanticResolver = mock(RecommendationSemanticResolver.class);

        resolutionService = new SemanticResolutionService(
                generationSemanticResolver, recommendationSemanticResolver
        );
    }

    @Test
    void quickGeneration_userProvidesAllAxes() {
        UnifiedGeneratePromptCommand command = UnifiedGeneratePromptCommand.of(
                1L, RequestMode.ADVANCED, PromptCategory.ETC, ActionIntent.GENERATE, null, "input", null,
                EngineMode.AUTO, ToneType.NEUTRAL, StyleType.NARRATIVE, LanguageType.KOREAN, ExperienceLevel.INTERMEDIATE,
                false, null, null, null, "title", "desc"
        );

        ConfirmedSemanticAxes axes = ConfirmedSemanticAxes.builder()
                .category(PromptCategory.ETC)
                .taskDomain(TaskDomain.GENERAL)
                .intent(ActionIntent.GENERATE)
                .objective(PromptObjective.CREATIVE_WITH_CONSTRAINTS)
                .outputNeeds(OutputNeeds.FREE_FORM)
                .tone(ToneType.NEUTRAL)
                .style(StyleType.NARRATIVE)
                .language(LanguageType.KOREAN)
                .experienceLevel(ExperienceLevel.INTERMEDIATE)
                .build();

        ResolutionResult.ResolutionMetadata metadata = new ResolutionResult.ResolutionMetadata(
                false, true, false, false, false
        );

        when(generationSemanticResolver.resolve(any(UnifiedGeneratePromptCommand.class)))
                .thenReturn(ResolutionResult.Result.ok(axes, metadata));

        ResolutionResult.Result result = resolutionService.resolve(command);

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

        RecommendPromptResult stubResult = new RecommendPromptResult(
                RequestMode.ADVANCED,
                PromptCategory.ETC,
                ActionIntent.GENERATE,
                List.of(ActionIntent.GENERATE, ActionIntent.ANALYZE),
                null,
                List.of(),
                null,
                List.of(),
                ToneType.NEUTRAL,
                StyleType.NARRATIVE,
                Map.of("intent", AxisSourceConstants.FALLBACK),
                List.of(),
                List.of(),
                List.of("intent: profile fallback applied"),
                ActionIntent.GENERATE.name()
        );

        when(recommendationSemanticResolver.resolveForRecommendation(any(RecommendPromptCommand.class)))
                .thenReturn(RecommendationResolutionResult.Result.ok(stubResult));

        RecommendPromptResult result = resolutionService.resolveForRecommendation(command);

        assertThat(result.recommendedIntent()).isEqualTo(ActionIntent.GENERATE);
        assertThat(result.fallbackApplied()).contains("intent: profile fallback applied");
        assertThat(result.intentCandidates()).containsExactly(ActionIntent.GENERATE, ActionIntent.ANALYZE);
        assertThat(result.axisSources()).containsEntry("intent", AxisSourceConstants.FALLBACK);
    }
}
