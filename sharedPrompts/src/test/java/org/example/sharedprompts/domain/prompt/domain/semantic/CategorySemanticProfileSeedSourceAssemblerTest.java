package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.DeserializerEnumTestUtils;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.DefaultCanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.writing.WritingRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.domain.semantic.impl.DefaultCategorySemanticProfileRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.impl.DefaultCategorySemanticProfileSeedSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.compatibility.CompatibilityPolicySource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.compatibility.DefaultCompatibilityPolicySource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 5 completion: profile seed source extraction.
 * Proves registry assembles from source, swapping source changes content, and compatibility override is preserved.
 */
@DisplayName("Category semantic profile seed source and assembler")
class CategorySemanticProfileSeedSourceAssemblerTest {

    private static final List<Class<? extends Enum<?>>> CATALOG = DeserializerEnumTestUtils.getActionTypeEnums();

    @Test
    @DisplayName("Registry assembles profiles from seed source, not inline data")
    void registryAssemblesFromSeedSource() {
        ActionTypeRegistry actionRegistry = new ActionTypeRegistry(CATALOG);
        CanonicalActionRegistry canonical = new DefaultCanonicalActionRegistry(actionRegistry);
        CategorySemanticProfileSeedSource defaultSource = new DefaultCategorySemanticProfileSeedSource();
        DefaultCategorySemanticProfileRegistry registry =
                new DefaultCategorySemanticProfileRegistry(canonical, null, defaultSource);

        CategorySemanticProfile profile = registry.getProfile(PromptCategory.WRITING).orElseThrow();
        assertThat(profile.getCategory()).isEqualTo(PromptCategory.WRITING);
        assertThat(profile.getBaseTaskDomain()).isEqualTo(TaskDomain.CREATIVE);
        assertThat(profile.getRecommendedRolesForIntent(ActionIntent.GENERATE))
                .containsExactlyInAnyOrderElementsOf(List.of(WritingRoleType.COPYWRITER, WritingRoleType.CONTENT_WRITER));
        assertThat(profile.getCompatibleActionsForIntent(ActionIntent.GENERATE)).isNotEmpty();
        assertThat(profile.getFallbackIntent()).isEqualTo(ActionIntent.GENERATE);
    }

    @Test
    @DisplayName("Swapping profile seed source changes assembled profile content")
    void swappingSeedSourceChangesAssembledContent() {
        ActionTypeRegistry actionRegistry = new ActionTypeRegistry(CATALOG);
        CanonicalActionRegistry canonical = new DefaultCanonicalActionRegistry(actionRegistry);

        CategorySemanticProfileSeedSource defaultSource = new DefaultCategorySemanticProfileSeedSource();
        DefaultCategorySemanticProfileRegistry registryDefault =
                new DefaultCategorySemanticProfileRegistry(canonical, null, defaultSource);
        CategorySemanticProfile profileDefault = registryDefault.getProfile(PromptCategory.WRITING).orElseThrow();
        List<RoleTypeInterface> defaultRoles = profileDefault.getRecommendedRolesForIntent(ActionIntent.GENERATE);

        // Custom source: same structure but different role order for WRITING+GENERATE (or different fallback)
        CategorySemanticProfileSeedSource customSource = new CategorySemanticProfileSeedSource() {
            @Override
            public Optional<CategorySemanticProfileSeed> getSeed(PromptCategory category) {
                if (category != PromptCategory.WRITING) {
                    return defaultSource.getSeed(category);
                }
                CategorySemanticProfileSeed original = defaultSource.getSeed(category).orElseThrow();
                Map<ActionIntent, List<RoleTypeInterface>> roles = new java.util.HashMap<>(original.rolesByIntent());
                roles.put(ActionIntent.GENERATE, List.of(WritingRoleType.TECHNICAL_WRITER, WritingRoleType.EDITOR));
                return Optional.of(new CategorySemanticProfileSeed(
                        original.category(), original.taskDomain(), original.allowedIntents(), original.intentFitLevels(),
                        roles, original.actionsByIntent(), original.discouragedTonesByIntent(), original.discouragedStylesByIntent(),
                        ActionIntent.REWRITE, original.fallbackCandidates()));
            }
        };
        DefaultCategorySemanticProfileRegistry registryCustom =
                new DefaultCategorySemanticProfileRegistry(canonical, null, customSource);
        CategorySemanticProfile profileCustom = registryCustom.getProfile(PromptCategory.WRITING).orElseThrow();

        assertThat(profileCustom.getRecommendedRolesForIntent(ActionIntent.GENERATE))
                .containsExactly(WritingRoleType.TECHNICAL_WRITER, WritingRoleType.EDITOR);
        assertThat(profileCustom.getFallbackIntent()).isEqualTo(ActionIntent.REWRITE);
        assertThat(profileCustom.getRecommendedRolesForIntent(ActionIntent.GENERATE))
                .isNotEqualTo(defaultRoles);
    }

    @Test
    @DisplayName("CompatibilityPolicySource override is reflected in assembled profile")
    void compatibilitySourceOverrideReflectedInProfile() {
        ActionTypeRegistry actionRegistry = new ActionTypeRegistry(CATALOG);
        CanonicalActionRegistry canonical = new DefaultCanonicalActionRegistry(actionRegistry);
        CompatibilityPolicySource compat = new DefaultCompatibilityPolicySource(
                Map.of("WRITING+GENERATE", List.of("LONG_FORM_WRITING", "CREATIVE_WRITING", "SHORT_COPY"))
        );
        DefaultCategorySemanticProfileRegistry registry =
                new DefaultCategorySemanticProfileRegistry(canonical, compat, new DefaultCategorySemanticProfileSeedSource());

        CategorySemanticProfile profile = registry.getProfile(PromptCategory.WRITING).orElseThrow();
        List<ActionGroup> groups = profile.getCompatibleActionGroupsForIntent(ActionIntent.GENERATE);

        assertThat(groups).contains(ActionGroup.LONG_FORM_WRITING, ActionGroup.CREATIVE_WRITING, ActionGroup.SHORT_COPY);
    }

    @Test
    @DisplayName("Default seed source returns seed for all profile categories, not EXTRACTION")
    void defaultSourceReturnsSeedsForProfileCategoriesOnly() {
        CategorySemanticProfileSeedSource source = new DefaultCategorySemanticProfileSeedSource();
        assertThat(source.getSeed(PromptCategory.EXTRACTION)).isEmpty();
        assertThat(source.getSeed(PromptCategory.WRITING)).isPresent();
        assertThat(source.getSeed(PromptCategory.DESIGN)).isPresent();
        assertThat(source.getSeed(PromptCategory.ETC)).isPresent();
    }

    @Test
    @DisplayName("Two-arg registry constructor uses default seed source and preserves behavior")
    void twoArgConstructorUsesDefaultSource() {
        ActionTypeRegistry actionRegistry = new ActionTypeRegistry(CATALOG);
        CanonicalActionRegistry canonical = new DefaultCanonicalActionRegistry(actionRegistry);
        DefaultCategorySemanticProfileRegistry registry = new DefaultCategorySemanticProfileRegistry(canonical);

        CategorySemanticProfile profile = registry.getProfile(PromptCategory.WRITING).orElseThrow();
        assertThat(profile.getCategory()).isEqualTo(PromptCategory.WRITING);
        assertThat(profile.getCompatibleActionGroupsForIntent(ActionIntent.GENERATE)).isNotEmpty();
    }
}
