package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.DeserializerEnumTestUtils;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.DefaultCanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.writing.WritingActionType;
import org.example.sharedprompts.domain.prompt.common.enums.engine.LanguageType;
import org.example.sharedprompts.domain.prompt.common.enums.experience.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.style.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.style.ToneType;
import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Permissive path: axes may omit action type; {@link ConfirmedSemanticAxes#findActionGroup} stays empty without failing.
 */
@DisplayName("ConfirmedSemanticAxes findActionGroup (permissive)")
class ConfirmedSemanticAxesFindActionGroupTest {

    private final CanonicalActionRegistry canonical =
            new DefaultCanonicalActionRegistry(new ActionTypeRegistry(DeserializerEnumTestUtils.getActionTypeEnums()));

    @Test
    @DisplayName("findActionGroup is empty when action type is absent (intent-only axes)")
    void findActionGroupEmptyWhenActionTypeAbsent() {
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
        assertThat(axes.actionType()).isEmpty();
        assertThat(axes.findActionGroup(canonical)).isEmpty();
    }

    @Test
    @DisplayName("findActionGroup resolves when action type is present")
    void findActionGroupPresentWhenActionTypeSet() {
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
        assertThat(axes.findActionGroup(canonical)).contains(ActionGroup.LONG_FORM_WRITING);
    }
}
