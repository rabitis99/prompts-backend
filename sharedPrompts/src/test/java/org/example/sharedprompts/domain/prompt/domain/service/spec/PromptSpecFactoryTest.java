package org.example.sharedprompts.domain.prompt.domain.service.spec;

import org.example.sharedprompts.domain.prompt.common.enums.DeserializerEnumTestUtils;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.DefaultCanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.writing.WritingActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.engine.LanguageType;
import org.example.sharedprompts.domain.prompt.common.enums.experience.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.style.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.style.ToneType;
import org.example.sharedprompts.domain.prompt.common.guideline.bundle.GuidelineBundleBuilder;
import org.example.sharedprompts.domain.prompt.domain.objective.ObjectiveRegistry;
import org.example.sharedprompts.domain.prompt.domain.objective.profiles.AnalyticalObjectiveProfile;
import org.example.sharedprompts.domain.prompt.domain.objective.profiles.CreativeObjectiveProfile;
import org.example.sharedprompts.domain.prompt.domain.objective.profiles.ExtractionObjectiveProfile;
import org.example.sharedprompts.domain.prompt.domain.objective.profiles.FactualObjectiveProfile;
import org.example.sharedprompts.domain.prompt.domain.objective.profiles.PlanningObjectiveProfile;
import org.example.sharedprompts.domain.prompt.domain.objective.profiles.ReasoningObjectiveProfile;
import org.example.sharedprompts.domain.prompt.domain.objective.registry.DefaultObjectiveRegistry;
import org.example.sharedprompts.domain.prompt.domain.policy.strategy.StrategyBundlePolicy;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveResolverPort;
import org.example.sharedprompts.domain.prompt.domain.semantic.ConfirmedSemanticAxes;
import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;
import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ActionGroup resolution for {@link PromptSpecFactory#createFromConfirmedAxes} uses
 * {@link CanonicalActionRegistry} only (no parallel {@link ActionTypeInterface#getActionGroup()} fallback).
 */
@DisplayName("PromptSpecFactory ActionGroup (registry-only)")
class PromptSpecFactoryTest {

    private ObjectiveRegistry objectiveRegistry;
    private StrategyBundlePolicy strategyBundlePolicy;
    private ObjectiveResolverPort objectiveResolver;
    private GuidelineBundleBuilder guidelineBundleBuilder;
    private CanonicalActionRegistry productionRegistry;

    @BeforeEach
    void setUp() {
        objectiveRegistry =
                new DefaultObjectiveRegistry(
                        List.of(
                                new FactualObjectiveProfile(),
                                new ReasoningObjectiveProfile(),
                                new ExtractionObjectiveProfile(),
                                new PlanningObjectiveProfile(),
                                new CreativeObjectiveProfile(),
                                new AnalyticalObjectiveProfile()));
        strategyBundlePolicy = new StrategyBundlePolicy(objectiveRegistry);
        objectiveResolver = mock(ObjectiveResolverPort.class);
        when(objectiveResolver.resolve(any(), any())).thenReturn(PromptObjective.FACTUAL);
        guidelineBundleBuilder = new GuidelineBundleBuilder();
        productionRegistry =
                new DefaultCanonicalActionRegistry(
                        new ActionTypeRegistry(DeserializerEnumTestUtils.getActionTypeEnums()));
    }

    @Test
    @DisplayName("createFromConfirmedAxes succeeds when action type resolves via CanonicalActionRegistry")
    void succeedsWhenActionResolvesViaRegistry() {
        PromptSpecFactory factory =
                new PromptSpecFactory(
                        objectiveRegistry,
                        strategyBundlePolicy,
                        objectiveResolver,
                        guidelineBundleBuilder,
                        null,
                        productionRegistry);

        ConfirmedSemanticAxes axes =
                ConfirmedSemanticAxes.builder()
                        .category(PromptCategory.WRITING)
                        .taskDomain(TaskDomain.CREATIVE)
                        .intent(ActionIntent.GENERATE)
                        .objective(PromptObjective.FACTUAL)
                        .outputNeeds(OutputNeeds.FREE_FORM)
                        .actionType(WritingActionType.ARTICLE_WRITING)
                        .tone(ToneType.NEUTRAL)
                        .style(StyleType.NARRATIVE)
                        .language(LanguageType.KOREAN)
                        .experienceLevel(ExperienceLevel.INTERMEDIATE)
                        .build();

        PromptSpec spec = factory.createFromConfirmedAxes(axes, "hello", null);
        assertThat(spec).isNotNull();
        assertThat(spec.getObjective()).isEqualTo(PromptObjective.FACTUAL);
    }

    @Test
    @DisplayName("createFromConfirmedAxes succeeds when action type is absent (no capability group required)")
    void succeedsWhenActionTypeAbsent() {
        PromptSpecFactory factory =
                new PromptSpecFactory(
                        objectiveRegistry,
                        strategyBundlePolicy,
                        objectiveResolver,
                        guidelineBundleBuilder,
                        null,
                        productionRegistry);

        ConfirmedSemanticAxes axes =
                ConfirmedSemanticAxes.builder()
                        .category(PromptCategory.WRITING)
                        .taskDomain(TaskDomain.CREATIVE)
                        .intent(ActionIntent.GENERATE)
                        .objective(PromptObjective.FACTUAL)
                        .outputNeeds(OutputNeeds.FREE_FORM)
                        .tone(ToneType.NEUTRAL)
                        .style(StyleType.NARRATIVE)
                        .language(LanguageType.KOREAN)
                        .experienceLevel(ExperienceLevel.INTERMEDIATE)
                        .build();

        PromptSpec spec = factory.createFromConfirmedAxes(axes, "hello", null);
        assertThat(spec).isNotNull();
        assertThat(spec.getActionType()).isNull();
    }

    @Test
    @DisplayName("createFromConfirmedAxes fails when action is present but registry does not resolve ActionGroup")
    void failsWhenRegistryDoesNotResolveActionGroup() {
        CanonicalActionRegistry emptyResolution = mock(CanonicalActionRegistry.class);
        when(emptyResolution.toCanonical(any(ActionTypeInterface.class))).thenReturn(Optional.empty());

        PromptSpecFactory factory =
                new PromptSpecFactory(
                        objectiveRegistry,
                        strategyBundlePolicy,
                        objectiveResolver,
                        guidelineBundleBuilder,
                        null,
                        emptyResolution);

        ConfirmedSemanticAxes axes =
                ConfirmedSemanticAxes.builder()
                        .category(PromptCategory.WRITING)
                        .taskDomain(TaskDomain.CREATIVE)
                        .intent(ActionIntent.GENERATE)
                        .objective(PromptObjective.FACTUAL)
                        .outputNeeds(OutputNeeds.FREE_FORM)
                        .actionType(WritingActionType.ARTICLE_WRITING)
                        .tone(ToneType.NEUTRAL)
                        .style(StyleType.NARRATIVE)
                        .language(LanguageType.KOREAN)
                        .experienceLevel(ExperienceLevel.INTERMEDIATE)
                        .build();

        assertThatThrownBy(() -> factory.createFromConfirmedAxes(axes, "hello", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CanonicalActionRegistry did not resolve ActionGroup")
                .hasMessageContaining(WritingActionType.ARTICLE_WRITING.key());
    }
}
