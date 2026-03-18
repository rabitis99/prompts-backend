package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.DeserializerEnumTestUtils;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.writing.WritingActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.DefaultCanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.domain.semantic.impl.DefaultCategorySemanticProfileRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that profile registry and compatibility are action-group-first:
 * profiles expose compatible action groups per intent, and concrete actions
 * that share an action group are treated as the same capability.
 */
@DisplayName("Profile action-group-first behavior")
class ProfileCanonicalFirstTest {

    private final CanonicalActionRegistry registry = new DefaultCanonicalActionRegistry(
            new ActionTypeRegistry(DeserializerEnumTestUtils.getActionTypeEnums()));
    private final DefaultCategorySemanticProfileRegistry profileRegistry =
            new DefaultCategorySemanticProfileRegistry(registry);

    @Test
    @DisplayName("profile returns non-empty action groups for intent when category has actions")
    void profileExposesActionGroupsForIntent() {
        var profile = profileRegistry.getProfile(PromptCategory.WRITING).orElseThrow();
        List<ActionGroup> forGenerate = profile.getCompatibleActionGroupsForIntent(ActionIntent.GENERATE);
        assertThat(forGenerate).isNotEmpty();
        assertThat(forGenerate).containsAnyOf(
                ActionGroup.LONG_FORM_WRITING,
                ActionGroup.CREATIVE_WRITING
        );
    }

    @Test
    @DisplayName("concrete actions mapping to same action group are both compatible for same intent")
    void sameActionGroupTreatedAsCompatible() {
        var profile = profileRegistry.getProfile(PromptCategory.WRITING).orElseThrow();
        List<ActionGroup> forRewrite = profile.getCompatibleActionGroupsForIntent(ActionIntent.REWRITE);
        assertThat(forRewrite).isNotEmpty();
        ActionGroup textRevision = ActionGroup.TEXT_REVISION;
        assertThat(forRewrite).contains(textRevision);
        assertThat(registry.toCanonical(WritingActionType.EDITING)).contains(textRevision);
        assertThat(registry.toCanonical(WritingActionType.PROOFREADING)).contains(textRevision);
    }

    @Test
    @DisplayName("getCompatibleActionsForIntent still returns concrete list for API compatibility")
    void concreteActionsStillReturnedForBackwardCompatibility() {
        var profile = profileRegistry.getProfile(PromptCategory.WRITING).orElseThrow();
        var concrete = profile.getCompatibleActionsForIntent(ActionIntent.GENERATE);
        assertThat(concrete).isNotEmpty();
        assertThat(concrete).contains(WritingActionType.ARTICLE_WRITING);
    }
}
