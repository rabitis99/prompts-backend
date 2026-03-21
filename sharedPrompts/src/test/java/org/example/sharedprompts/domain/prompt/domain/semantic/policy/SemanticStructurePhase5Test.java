package org.example.sharedprompts.domain.prompt.domain.semantic.policy;

import org.example.sharedprompts.domain.prompt.common.enums.DeserializerEnumTestUtils;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.DefaultCanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveMappingRegistry;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveResolver;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveResolverPort;
import org.example.sharedprompts.domain.prompt.domain.resolutions.DefaultObjectiveHeuristicInferencePolicy;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfile;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileSeedSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.RecommendationResult;
import org.example.sharedprompts.domain.prompt.domain.semantic.impl.DefaultCategorySemanticProfileRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.impl.DefaultCategorySemanticProfileSeedSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.compatibility.CompatibilityPolicySource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.compatibility.DefaultCompatibilityPolicySource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.objective.DefaultObjectivePolicySource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.objective.ObjectivePolicySource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.ActionRecommendationPreferenceSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.DefaultActionRecommendationPreferenceSource;
import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicySelectionStrategy;
import org.example.sharedprompts.domain.prompt.application.semantic.recommendation.PolicyTestFixtures;
import org.example.sharedprompts.domain.prompt.application.semantic.recommendation.SemanticRecommendationService;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.registry.PolicySourceRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 5차 구조 리팩터링 검증: data-driven policy source, UX key alignment,
 * config에 raw map 없음, source 교체만으로 동작 변경 가능.
 */
@DisplayName("Semantic structure phase 5: policy sources, UX keys, no raw map in config")
class SemanticStructurePhase5Test {

    private static final List<Class<? extends Enum<?>>> CATALOG = DeserializerEnumTestUtils.getActionTypeEnums();

    @Test
    @DisplayName("Recommendation order changes when preference source is swapped without service code change")
    void recommendationOrderChangesWhenPreferenceSourceSwapped() {
        ActionTypeRegistry registry = new ActionTypeRegistry(CATALOG);
        CanonicalActionRegistry canonical = new DefaultCanonicalActionRegistry(registry);
        org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.ConcreteActionByGroupIndex index =
                new org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.DefaultConcreteActionByGroupIndex(registry.getAll());
        org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.RecommendationConcreteActionExpander expander =
                new org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.DefaultRecommendationConcreteActionExpander(index);

        DefaultCategorySemanticProfileRegistry profileRegistry = new DefaultCategorySemanticProfileRegistry(
                canonical, new DefaultCategorySemanticProfileSeedSource());
        CategorySemanticProfile profile = profileRegistry.getProfile(PromptCategory.WRITING).orElseThrow();

        PolicySourceRegistry registryA = PolicyTestFixtures.registryWithSingleVersion(new DefaultActionRecommendationPreferenceSource());
        PolicySelectionStrategy strategy = PolicyTestFixtures.singleVersionStrategy();
        SemanticRecommendationService serviceA = new SemanticRecommendationService(canonical, expander, registryA, strategy);

        PolicySourceRegistry registryB = PolicyTestFixtures.registryWithSingleVersion(
                new DefaultActionRecommendationPreferenceSource(
                        Map.of("WRITING+GENERATE", List.of("ACTION.WRITING.COPYWRITING", "ACTION.WRITING.ARTICLE_WRITING"))
                ));
        SemanticRecommendationService serviceB = new SemanticRecommendationService(canonical, expander, registryB, strategy);

        RecommendationResult resultA = serviceA.recommend(PromptCategory.WRITING, ActionIntent.GENERATE, profile, null, null, false);
        RecommendationResult resultB = serviceB.recommend(PromptCategory.WRITING, ActionIntent.GENERATE, profile, null, null, false);

        assertThat(resultA.actionCandidates()).isNotEmpty();
        assertThat(resultB.actionCandidates()).containsExactlyInAnyOrderElementsOf(resultA.actionCandidates());
        if (resultA.actionCandidates().size() >= 2 && resultB.actionCandidates().size() >= 2) {
            assertThat(resultB.actionCandidates().get(0).key()).isEqualTo("ACTION.WRITING.COPYWRITING");
        }
    }

    @Test
    @DisplayName("Objective resolution uses policy source; swapping source changes mapping")
    void objectiveResolutionUsesPolicySource() {
        ActionTypeRegistry registry = new ActionTypeRegistry(CATALOG);

        ObjectivePolicySource sourceDefault = new DefaultObjectivePolicySource();
        ObjectiveMappingRegistry registryDefault =
                new ObjectiveMappingRegistry(sourceDefault, new DefaultObjectiveHeuristicInferencePolicy());
        ObjectiveResolverPort resolverDefault = new ObjectiveResolver(registryDefault);

        Map<String, PromptObjective> customMap = Map.of(
                "ACTION.CODING.CODE_REVIEW", PromptObjective.ANALYTICAL,
                "ACTION.WRITING.TRANSLATION", PromptObjective.CREATIVE_WITH_CONSTRAINTS
        );
        ObjectivePolicySource sourceCustom = new DefaultObjectivePolicySource(customMap);
        ObjectiveMappingRegistry registryCustom =
                new ObjectiveMappingRegistry(sourceCustom, new DefaultObjectiveHeuristicInferencePolicy());
        ObjectiveResolverPort resolverCustom = new ObjectiveResolver(registryCustom);

        ActionTypeInterface codeReview = registry.getByStableKey("ACTION.CODING.CODE_REVIEW");
        ActionTypeInterface translation = registry.getByStableKey("ACTION.WRITING.TRANSLATION");
        if (codeReview == null || translation == null) return;

        assertThat(resolverDefault.resolve(TaskDomain.TECHNICAL, codeReview)).isEqualTo(PromptObjective.REASONING);
        assertThat(resolverCustom.resolve(TaskDomain.TECHNICAL, codeReview)).isEqualTo(PromptObjective.ANALYTICAL);
        assertThat(resolverDefault.resolve(TaskDomain.CREATIVE, translation)).isEqualTo(PromptObjective.FACTUAL);
        assertThat(resolverCustom.resolve(TaskDomain.CREATIVE, translation)).isEqualTo(PromptObjective.CREATIVE_WITH_CONSTRAINTS);
    }

    @Test
    @DisplayName("Category and Intent are first-class inputs to preference source")
    void categoryAndIntentFirstClassInPreferenceSource() {
        ActionRecommendationPreferenceSource source = new DefaultActionRecommendationPreferenceSource(
                Map.of(
                        "WRITING+GENERATE", List.of("A", "B"),
                        "DEVELOPMENT+DIAGNOSE", List.of("C")
                )
        );
        assertThat(source.getPreferredActionKeys(PromptCategory.WRITING, ActionIntent.GENERATE)).containsExactly("A", "B");
        assertThat(source.getPreferredActionKeys(PromptCategory.DEVELOPMENT, ActionIntent.DIAGNOSE)).containsExactly("C");
        assertThat(source.getPreferredActionKeys(PromptCategory.WRITING, ActionIntent.DIAGNOSE)).isEmpty();
        assertThat(source.getPreferredActionKeys(PromptCategory.DEVELOPMENT, ActionIntent.GENERATE)).isEmpty();
    }

    @Test
    @DisplayName("CompatibilityPolicySource returns group keys by category and intent")
    void compatibilitySourceByCategoryAndIntent() {
        CompatibilityPolicySource source = new DefaultCompatibilityPolicySource(
                Map.of(
                        "WRITING+GENERATE", List.of("LONG_FORM_WRITING", "CREATIVE_WRITING"),
                        "DEVELOPMENT+GENERATE", List.of("CODE_GENERATION")
                )
        );
        assertThat(source.getCompatibleGroupKeys(PromptCategory.WRITING, ActionIntent.GENERATE))
                .containsExactly("LONG_FORM_WRITING", "CREATIVE_WRITING");
        assertThat(source.getCompatibleGroupKeys(PromptCategory.DEVELOPMENT, ActionIntent.GENERATE))
                .containsExactly("CODE_GENERATION");
        assertThat(source.getCompatibleGroupKeys(PromptCategory.WRITING, ActionIntent.REWRITE)).isEmpty();
    }

    @Test
    @DisplayName("Profile registry uses CompatibilityPolicySource when provided and returns non-empty")
    void profileRegistryUsesCompatibilitySourceWhenProvided() {
        ActionTypeRegistry registry = new ActionTypeRegistry(CATALOG);
        CanonicalActionRegistry canonical = new DefaultCanonicalActionRegistry(registry);
        CompatibilityPolicySource compatibilitySource = new DefaultCompatibilityPolicySource(
                Map.of("WRITING+GENERATE", List.of("LONG_FORM_WRITING", "CREATIVE_WRITING", "SHORT_COPY"))
        );
        DefaultCategorySemanticProfileRegistry profileRegistryWithSource =
                new DefaultCategorySemanticProfileRegistry(canonical, compatibilitySource, new DefaultCategorySemanticProfileSeedSource());
        DefaultCategorySemanticProfileRegistry profileRegistryNoSource =
                new DefaultCategorySemanticProfileRegistry(canonical, new DefaultCategorySemanticProfileSeedSource());

        CategorySemanticProfile profileWith = profileRegistryWithSource.getProfile(PromptCategory.WRITING).orElseThrow();
        CategorySemanticProfile profileWithout = profileRegistryNoSource.getProfile(PromptCategory.WRITING).orElseThrow();

        List<ActionGroup> groupsWith = profileWith.getCompatibleActionGroupsForIntent(ActionIntent.GENERATE);
        List<ActionGroup> groupsWithout = profileWithout.getCompatibleActionGroupsForIntent(ActionIntent.GENERATE);

        assertThat(groupsWith).contains(ActionGroup.LONG_FORM_WRITING, ActionGroup.CREATIVE_WRITING, ActionGroup.SHORT_COPY);
        assertThat(groupsWithout).isNotEmpty();
        assertThat(groupsWith).isNotEqualTo(groupsWithout);
    }

    @Test
    @DisplayName("Objective policy source resolves by stable key only")
    void objectivePolicySourceStableKeyOnly() {
        ObjectivePolicySource source = new DefaultObjectivePolicySource(
                Map.of("ACTION.CODING.DEBUGGING", PromptObjective.REASONING)
        );
        assertThat(source.findByStableKey("ACTION.CODING.DEBUGGING")).contains(PromptObjective.REASONING);
        assertThat(source.findByStableKey("ACTION.UNKNOWN.NEW_ACTION")).isEmpty();
        assertThat(source.getDomainDefault(TaskDomain.ANALYTICAL)).isEqualTo(PromptObjective.ANALYTICAL);
    }

    @Test
    @DisplayName("Profile registry assembles from seed source; no profile when source returns empty for category")
    void profileRegistryAssemblesFromSeedSourceOnly() {
        ActionTypeRegistry registry = new ActionTypeRegistry(CATALOG);
        CanonicalActionRegistry canonical = new DefaultCanonicalActionRegistry(registry);
        DefaultCategorySemanticProfileSeedSource fullSource = new DefaultCategorySemanticProfileSeedSource();
        // Source that omits WRITING: only DESIGN
        CategorySemanticProfileSeedSource partialSource = category -> {
            if (category == PromptCategory.WRITING) return java.util.Optional.empty();
            return fullSource.getSeed(category);
        };
        DefaultCategorySemanticProfileRegistry registryFull =
                new DefaultCategorySemanticProfileRegistry(canonical, null, fullSource);
        DefaultCategorySemanticProfileRegistry registryPartial =
                new DefaultCategorySemanticProfileRegistry(canonical, null, partialSource);

        assertThat(registryFull.getProfile(PromptCategory.WRITING)).isPresent();
        assertThat(registryPartial.getProfile(PromptCategory.WRITING)).isEmpty();
        assertThat(registryPartial.getProfile(PromptCategory.DESIGN)).isPresent();
    }

    @Test
    @DisplayName("ResolutionConfig does not declare EXPLICIT_MAPPINGS or raw Map.ofEntries for objective")
    void configDoesNotHoldRawObjectiveMap() {
        String path = "org/example/sharedprompts/domain/prompt/infrastructure/config/ResolutionConfig.class";
        java.net.URL resource = getClass().getClassLoader().getResource(path);
        assertThat(resource).isNotNull();
        // Structural: ResolutionConfig bean methods wire ObjectivePolicySource, not a local Map
        Class<?> configClass;
        try {
            configClass = Class.forName("org.example.sharedprompts.domain.prompt.infrastructure.config.ResolutionConfig");
        } catch (ClassNotFoundException e) {
            throw new AssertionError("ResolutionConfig not loadable", e);
        }
        java.lang.reflect.Method[] methods = configClass.getDeclaredMethods();
        boolean hasObjectivePolicySourceBean = false;
        boolean hasExplicitMappingsField = false;
        for (java.lang.reflect.Method m : methods) {
            if (m.getName().equals("objectivePolicySource")) hasObjectivePolicySourceBean = true;
        }
        for (java.lang.reflect.Field f : configClass.getDeclaredFields()) {
            if (f.getName().contains("EXPLICIT_MAPPINGS") || f.getName().contains("EXPLICIT_MAPPINGS_BY_KEY")) {
                hasExplicitMappingsField = true;
                break;
            }
        }
        assertThat(hasObjectivePolicySourceBean).as("ResolutionConfig should expose objectivePolicySource bean").isTrue();
        assertThat(hasExplicitMappingsField).as("ResolutionConfig must not hold EXPLICIT_MAPPINGS raw map").isFalse();
    }
}
