package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.DeserializerEnumTestUtils;
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
import org.example.sharedprompts.domain.prompt.domain.semantic.seed.CategorySemanticProfileSeedDefinitions;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.compatibility.CompatibilityPolicySource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.compatibility.DefaultCompatibilityPolicySource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 5 completion: profile seed source extraction.
 * Proves registry assembles from source, swapping source changes content, and compatibility override is preserved.
 */
@DisplayName("Category semantic profile seed source and assembler")
class CategorySemanticProfileSeedSourceAssemblerTest {

    private static final List<Class<? extends Enum<?>>> CATALOG = DeserializerEnumTestUtils.getActionTypeEnums();

    @Test
    @DisplayName("Every profileCategoriesForRegistry entry gets a built profile when seeds are complete")
    void registryHasProfileForEachProfileCategoryWhenSeedsComplete() {
        ActionTypeRegistry actionRegistry = new ActionTypeRegistry(CATALOG);
        CanonicalActionRegistry canonical = new DefaultCanonicalActionRegistry(actionRegistry);
        CategorySemanticProfileSeedSource source = new DefaultCategorySemanticProfileSeedSource();
        DefaultCategorySemanticProfileRegistry registry =
                new DefaultCategorySemanticProfileRegistry(canonical, null, source);
        for (PromptCategory c : source.profileCategoriesForRegistry()) {
            assertThat(registry.getProfile(c)).as("profile for %s", c).isPresent();
        }
    }

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
            public CategorySemanticProfileSeed requireSeed(PromptCategory category) {
                if (category.canonical() != PromptCategory.WRITING) {
                    return defaultSource.requireSeed(category);
                }
                CategorySemanticProfileSeed original = defaultSource.requireSeed(category);
                Map<ActionIntent, List<RoleTypeInterface>> roles = new java.util.HashMap<>(original.rolesByIntent());
                roles.put(ActionIntent.GENERATE, List.of(WritingRoleType.TECHNICAL_WRITER, WritingRoleType.EDITOR));
                return new CategorySemanticProfileSeed(
                        original.category(), original.taskDomain(), original.allowedIntents(), original.intentFitLevels(),
                        roles, original.actionsByIntent(), original.discouragedTonesByIntent(), original.discouragedStylesByIntent(),
                        ActionIntent.REWRITE, original.fallbackCandidates());
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
    @DisplayName("Default seed source covers all canonical profile categories; EXTRACTION has no seed")
    void defaultSourceReturnsSeedsForProfileCategoriesOnly() {
        CategorySemanticProfileSeedSource source = new DefaultCategorySemanticProfileSeedSource();
        assertThatThrownBy(() -> source.requireSeed(PromptCategory.EXTRACTION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EXTRACTION")
                .hasMessageContaining("Missing CategorySemanticProfileSeed")
                .hasMessageContaining("no seed supplier registered");
        assertThat(source.requireSeed(PromptCategory.WRITING).category()).isEqualTo(PromptCategory.WRITING);
        assertThat(source.requireSeed(PromptCategory.DESIGN).category()).isEqualTo(PromptCategory.DESIGN);
        assertThat(source.requireSeed(PromptCategory.ETC).category()).isEqualTo(PromptCategory.ETC);
    }

    @Test
    @DisplayName("Custom source narrows profileCategoriesForRegistry when only a subset of profiles is wired")
    void customSourceSubsetProfileCategoriesBuildsPartialRegistry() {
        ActionTypeRegistry actionRegistry = new ActionTypeRegistry(CATALOG);
        CanonicalActionRegistry canonical = new DefaultCanonicalActionRegistry(actionRegistry);
        DefaultCategorySemanticProfileSeedSource fullSource = new DefaultCategorySemanticProfileSeedSource();
        CategorySemanticProfileSeedSource writingOnly = new CategorySemanticProfileSeedSource() {
            @Override
            public Set<PromptCategory> profileCategoriesForRegistry() {
                return Set.of(PromptCategory.WRITING);
            }

            @Override
            public CategorySemanticProfileSeed requireSeed(PromptCategory category) {
                return fullSource.requireSeed(category);
            }
        };
        DefaultCategorySemanticProfileRegistry registry =
                new DefaultCategorySemanticProfileRegistry(canonical, null, writingOnly);

        assertThat(registry.getProfile(PromptCategory.WRITING)).isPresent();
        assertThat(registry.getProfile(PromptCategory.DESIGN)).isEmpty();
    }

    @Test
    @DisplayName("DefaultCategorySemanticProfileSeedSource rejects an incomplete definition catalog at construction")
    void seedSourceConstructorFailsFastWhenDefinitionMissingForEnumCategory() {
        List<CategorySemanticProfileSeedDefinition> incomplete =
                CategorySemanticProfileSeedDefinitions.defaultDefinitions().stream()
                        .filter(d -> d.category() != PromptCategory.DESIGN)
                        .toList();
        assertThatThrownBy(() -> new DefaultCategorySemanticProfileSeedSource(incomplete))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DESIGN")
                .hasMessageContaining("missing");
    }

    @Test
    @DisplayName("Registry requires external seed source (no internal default new)")
    void registryRequiresExternalSeedSource() {
        ActionTypeRegistry actionRegistry = new ActionTypeRegistry(CATALOG);
        CanonicalActionRegistry canonical = new DefaultCanonicalActionRegistry(actionRegistry);
        DefaultCategorySemanticProfileSeedSource defaultSource = new DefaultCategorySemanticProfileSeedSource();
        DefaultCategorySemanticProfileRegistry registry = new DefaultCategorySemanticProfileRegistry(canonical, defaultSource);

        CategorySemanticProfile profile = registry.getProfile(PromptCategory.WRITING).orElseThrow();
        assertThat(profile.getCategory()).isEqualTo(PromptCategory.WRITING);
        assertThat(profile.getCompatibleActionGroupsForIntent(ActionIntent.GENERATE)).isNotEmpty();
    }
}
