package org.example.sharedprompts.domain.prompt.domain.semantic.impl;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.DefaultCanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileSeed;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileSeedSource;
import org.example.sharedprompts.domain.prompt.common.enums.DeserializerEnumTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fail-fast for {@link DefaultCategorySemanticProfileRegistry} intent → action group mapping
 * (no silent drop when an intent key exists but has no resolvable capability groups).
 */
@DisplayName("DefaultCategorySemanticProfileRegistry toActionGroupMap fail-fast")
class DefaultCategorySemanticProfileRegistryFailFastTest {

    private static final List<Class<? extends Enum<?>>> CATALOG = DeserializerEnumTestUtils.getActionTypeEnums();

    @Test
    @DisplayName("throws when intent has empty action list (category + intent in message)")
    void throwsWhenIntentHasEmptyActionList() {
        CanonicalActionRegistry canonical =
                new DefaultCanonicalActionRegistry(new ActionTypeRegistry(CATALOG));
        CategorySemanticProfileSeed base =
                new DefaultCategorySemanticProfileSeedSource().requireSeed(PromptCategory.WRITING);
        Map<ActionIntent, List<ActionTypeInterface>> actions = new HashMap<>(base.actionsByIntent());
        actions.put(ActionIntent.GENERATE, List.of());
        CategorySemanticProfileSeed badSeed =
                new CategorySemanticProfileSeed(
                        base.category(),
                        base.taskDomain(),
                        base.allowedIntents(),
                        base.intentFitLevels(),
                        base.rolesByIntent(),
                        actions,
                        base.discouragedTonesByIntent(),
                        base.discouragedStylesByIntent(),
                        base.fallbackIntent(),
                        base.fallbackCandidates());

        CategorySemanticProfileSeedSource source =
                new CategorySemanticProfileSeedSource() {
                    @Override
                    public Set<PromptCategory> profileCategoriesForRegistry() {
                        return Set.of(PromptCategory.WRITING);
                    }

                    @Override
                    public CategorySemanticProfileSeed requireSeed(PromptCategory category) {
                        return badSeed;
                    }
                };

        assertThatThrownBy(() -> new DefaultCategorySemanticProfileRegistry(canonical, null, source))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category semantic profile seed contains empty action list")
                .hasMessageContaining("WRITING")
                .hasMessageContaining("GENERATE");
    }

    @Test
    @DisplayName("throws when resolved ActionGroup list is empty (category + intent in message)")
    void throwsWhenResolvedActionGroupListEmpty() {
        assertThatThrownBy(
                () ->
                        DefaultCategorySemanticProfileRegistry.assertIntentHasResolvedActionGroups(
                                PromptCategory.WRITING, ActionIntent.GENERATE, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No ActionGroup resolved")
                .hasMessageContaining("WRITING")
                .hasMessageContaining("GENERATE");
    }
}
