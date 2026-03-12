package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.action.category.writing.WritingActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionId;
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
 * Verifies that profile registry and compatibility are canonical-first:
 * profiles expose compatible canonical actions per intent, and concrete actions
 * that share a canonical are treated as the same capability.
 */
@DisplayName("Profile canonical-first behavior")
class ProfileCanonicalFirstTest {

    private final CanonicalActionRegistry registry = new DefaultCanonicalActionRegistry();
    private final DefaultCategorySemanticProfileRegistry profileRegistry =
            new DefaultCategorySemanticProfileRegistry(registry);

    @Test
    @DisplayName("profile returns non-empty canonical actions for intent when category has actions")
    void profileExposesCanonicalActionsForIntent() {
        var profile = profileRegistry.getProfile(PromptCategory.WRITING).orElseThrow();
        List<CanonicalActionId> forGenerate = profile.getCompatibleCanonicalActionsForIntent(ActionIntent.GENERATE);
        assertThat(forGenerate).isNotEmpty();
        assertThat(forGenerate).containsAnyOf(
                CanonicalActionId.LONG_FORM_WRITING,
                CanonicalActionId.CREATIVE_WRITING
        );
    }

    @Test
    @DisplayName("concrete actions mapping to same canonical are both compatible for same intent")
    void sameCanonicalTreatedAsCompatible() {
        var profile = profileRegistry.getProfile(PromptCategory.WRITING).orElseThrow();
        List<CanonicalActionId> forRewrite = profile.getCompatibleCanonicalActionsForIntent(ActionIntent.REWRITE);
        assertThat(forRewrite).isNotEmpty();
        CanonicalActionId textRevision = CanonicalActionId.TEXT_REVISION;
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
