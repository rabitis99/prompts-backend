package org.example.sharedprompts.domain.prompt.domain.semantic.policy;

import org.example.sharedprompts.domain.prompt.common.enums.DeserializerEnumTestUtils;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.DefaultCanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfile;
import org.example.sharedprompts.domain.prompt.domain.semantic.RecommendationResult;
import org.example.sharedprompts.domain.prompt.domain.semantic.impl.DefaultCategorySemanticProfileRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.impl.DefaultCategorySemanticProfileSeedSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.ActionRecommendationOrderPolicy;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.ActionRecommendationPreferenceSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.ConcreteActionByGroupIndex;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.DefaultActionRecommendationOrderPolicy;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.DefaultActionRecommendationPreferenceSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.DefaultConcreteActionByGroupIndex;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.DefaultRecommendationConcreteActionExpander;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.RecommendationConcreteActionExpander;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicySelectionStrategy;
import org.example.sharedprompts.domain.prompt.application.semantic.recommendation.PolicyTestFixtures;
import org.example.sharedprompts.domain.prompt.application.semantic.recommendation.SemanticRecommendationService;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.registry.PolicySourceRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.version.PolicyVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 4차 구조 리팩터링 검증: semantic compatibility와 concrete recommendation 분리,
 * 추천 순서는 명시적 정책으로만 결정, service는 orchestration만 수행.
 */
@DisplayName("Semantic structure phase 4: compatibility vs recommendation, explicit order policy")
class SemanticStructurePhase4Test {

    private static final List<Class<? extends Enum<?>>> CATALOG = DeserializerEnumTestUtils.getActionTypeEnums();

    @Test
    @DisplayName("Same compatible groups yield different concrete order when preference policy differs")
    void compatibilityVsRecommendationSeparated() {
        ActionTypeRegistry registry = new ActionTypeRegistry(CATALOG);
        ConcreteActionByGroupIndex index = new DefaultConcreteActionByGroupIndex(registry.getAll());
        RecommendationConcreteActionExpander expander = new DefaultRecommendationConcreteActionExpander(index);

        DefaultCategorySemanticProfileRegistry profileRegistry = new DefaultCategorySemanticProfileRegistry(
                new DefaultCanonicalActionRegistry(registry),
                new DefaultCategorySemanticProfileSeedSource());
        CategorySemanticProfile profile = profileRegistry.getProfile(PromptCategory.WRITING).orElseThrow();
        Set<ActionGroup> groups = Set.copyOf(profile.getCompatibleActionGroupsForIntent(ActionIntent.GENERATE));
        List<ActionTypeInterface> seeds = profile.getCompatibleActionsForIntent(ActionIntent.GENERATE);

        List<ActionTypeInterface> expanded = expander.expand(PromptCategory.WRITING, ActionIntent.GENERATE, groups, seeds);
        assertThat(expanded).isNotEmpty();

        ActionRecommendationPreferenceSource emptyPref = new DefaultActionRecommendationPreferenceSource(Map.of());
        ActionRecommendationOrderPolicy policyEmpty = new DefaultActionRecommendationOrderPolicy(emptyPref);
        List<ActionTypeInterface> orderEmpty = policyEmpty.applyOrder(PromptCategory.WRITING, ActionIntent.GENERATE, expanded);

        String keyFirst = orderEmpty.get(0).key();
        ActionRecommendationPreferenceSource customPref = new DefaultActionRecommendationPreferenceSource(
                Map.of("WRITING+GENERATE", List.of(orderEmpty.get(orderEmpty.size() - 1).key(), keyFirst))
        );
        ActionRecommendationOrderPolicy policyCustom = new DefaultActionRecommendationOrderPolicy(customPref);
        List<ActionTypeInterface> orderCustom = policyCustom.applyOrder(PromptCategory.WRITING, ActionIntent.GENERATE, expanded);

        assertThat(orderCustom).containsExactlyInAnyOrderElementsOf(orderEmpty);
        assertThat(orderCustom.get(0).key()).isEqualTo(orderEmpty.get(orderEmpty.size() - 1).key());
    }

    @Test
    @DisplayName("Recommendation order is determined only by order policy, not seed declaration order")
    void orderFromExplicitPolicyOnly() {
        ActionTypeRegistry registry = new ActionTypeRegistry(CATALOG);
        ConcreteActionByGroupIndex index = new DefaultConcreteActionByGroupIndex(registry.getAll());
        RecommendationConcreteActionExpander expander = new DefaultRecommendationConcreteActionExpander(index);
        ActionRecommendationPreferenceSource pref = new DefaultActionRecommendationPreferenceSource(Map.of());
        ActionRecommendationOrderPolicy orderPolicy = new DefaultActionRecommendationOrderPolicy(pref);

        DefaultCategorySemanticProfileRegistry profileRegistry = new DefaultCategorySemanticProfileRegistry(
                new DefaultCanonicalActionRegistry(registry),
                new DefaultCategorySemanticProfileSeedSource());
        CategorySemanticProfile profile = profileRegistry.getProfile(PromptCategory.WRITING).orElseThrow();
        List<ActionTypeInterface> expanded = expander.expand(
                PromptCategory.WRITING,
                ActionIntent.GENERATE,
                Set.copyOf(profile.getCompatibleActionGroupsForIntent(ActionIntent.GENERATE)),
                profile.getCompatibleActionsForIntent(ActionIntent.GENERATE)
        );
        List<ActionTypeInterface> ordered = orderPolicy.applyOrder(PromptCategory.WRITING, ActionIntent.GENERATE, expanded);

        List<Class<? extends Enum<?>>> reversedCatalog = new ArrayList<>(CATALOG);
        Collections.reverse(reversedCatalog);
        ActionTypeRegistry registryReversed = new ActionTypeRegistry(reversedCatalog);
        ConcreteActionByGroupIndex indexReversed = new DefaultConcreteActionByGroupIndex(registryReversed.getAll());
        RecommendationConcreteActionExpander expanderReversed = new DefaultRecommendationConcreteActionExpander(indexReversed);
        List<ActionTypeInterface> expandedReversed = expanderReversed.expand(
                PromptCategory.WRITING,
                ActionIntent.GENERATE,
                Set.copyOf(profile.getCompatibleActionGroupsForIntent(ActionIntent.GENERATE)),
                profile.getCompatibleActionsForIntent(ActionIntent.GENERATE)
        );
        List<ActionTypeInterface> orderedReversed = orderPolicy.applyOrder(PromptCategory.WRITING, ActionIntent.GENERATE, expandedReversed);

        assertThat(orderedReversed).extracting(ActionTypeInterface::key).containsExactlyElementsOf(ordered.stream().map(ActionTypeInterface::key).toList());
    }

    @Test
    @DisplayName("Expander does not depend on registry getAll() or catalog order for membership")
    void expanderIndependentOfRegistryOrder() {
        ActionTypeRegistry registry = new ActionTypeRegistry(CATALOG);
        ConcreteActionByGroupIndex index = new DefaultConcreteActionByGroupIndex(registry.getAll());
        RecommendationConcreteActionExpander expander = new DefaultRecommendationConcreteActionExpander(index);

        DefaultCategorySemanticProfileRegistry profileRegistry = new DefaultCategorySemanticProfileRegistry(
                new DefaultCanonicalActionRegistry(registry),
                new DefaultCategorySemanticProfileSeedSource());
        CategorySemanticProfile profile = profileRegistry.getProfile(PromptCategory.DEVELOPMENT).orElseThrow();
        Set<ActionGroup> groups = Set.copyOf(profile.getCompatibleActionGroupsForIntent(ActionIntent.GENERATE));

        List<ActionTypeInterface> expanded = expander.expand(PromptCategory.DEVELOPMENT, ActionIntent.GENERATE, groups, null);
        List<String> keysA = expanded.stream().map(ActionTypeInterface::key).sorted().toList();

        List<Class<? extends Enum<?>>> reversedCatalog = new ArrayList<>(CATALOG);
        Collections.reverse(reversedCatalog);
        ConcreteActionByGroupIndex indexReversed = new DefaultConcreteActionByGroupIndex(new ActionTypeRegistry(reversedCatalog).getAll());
        RecommendationConcreteActionExpander expanderReversed = new DefaultRecommendationConcreteActionExpander(indexReversed);
        List<ActionTypeInterface> expandedReversed = expanderReversed.expand(PromptCategory.DEVELOPMENT, ActionIntent.GENERATE, groups, null);
        List<String> keysB = expandedReversed.stream().map(ActionTypeInterface::key).sorted().toList();

        assertThat(keysB).containsExactlyInAnyOrderElementsOf(keysA);
    }

    @Test
    @DisplayName("Representative concrete action can differ by category+intent via preference")
    void representativeActionVariesByContext() {
        ActionTypeRegistry registry = new ActionTypeRegistry(CATALOG);
        CanonicalActionRegistry canonical = new DefaultCanonicalActionRegistry(registry);
        ConcreteActionByGroupIndex index = new DefaultConcreteActionByGroupIndex(registry.getAll());
        RecommendationConcreteActionExpander expander = new DefaultRecommendationConcreteActionExpander(index);

        DefaultCategorySemanticProfileRegistry profileRegistry = new DefaultCategorySemanticProfileRegistry(
                canonical, new DefaultCategorySemanticProfileSeedSource());
        SemanticRecommendationService serviceWriting = serviceWithPreference(Map.of(
                "WRITING+GENERATE", List.of("ACTION.WRITING.ARTICLE_WRITING", "ACTION.WRITING.CREATIVE_WRITING_GEN")
        ), index, expander);
        SemanticRecommendationService serviceDevelopment = serviceWithPreference(Map.of(
                "DEVELOPMENT+GENERATE", List.of("ACTION.CODING.CODE_GENERATION", "ACTION.CODING.CODE_MODIFICATION")
        ), index, expander);

        CategorySemanticProfile profileW = profileRegistry.getProfile(PromptCategory.WRITING).orElseThrow();
        CategorySemanticProfile profileD = profileRegistry.getProfile(PromptCategory.DEVELOPMENT).orElseThrow();

        RecommendationResult resultW = serviceWriting.recommend(PromptCategory.WRITING, ActionIntent.GENERATE, profileW, null, null, false);
        RecommendationResult resultD = serviceDevelopment.recommend(PromptCategory.DEVELOPMENT, ActionIntent.GENERATE, profileD, null, null, false);

        assertThat(resultW.recommendedAction()).isPresent();
        assertThat(resultD.recommendedAction()).isPresent();
        assertThat(resultW.recommendedAction().get().key()).isNotEqualTo(resultD.recommendedAction().get().key());
    }

    @Test
    @DisplayName("Adding stable-key preference changes recommendation order")
    void stableKeyPreferenceChangesOrder() {
        ActionTypeRegistry registry = new ActionTypeRegistry(CATALOG);
        ConcreteActionByGroupIndex index = new DefaultConcreteActionByGroupIndex(registry.getAll());
        RecommendationConcreteActionExpander expander = new DefaultRecommendationConcreteActionExpander(index);

        ActionRecommendationPreferenceSource noPref = new DefaultActionRecommendationPreferenceSource(Map.of());
        ActionRecommendationOrderPolicy policyNoPref = new DefaultActionRecommendationOrderPolicy(noPref);

        DefaultCategorySemanticProfileRegistry profileRegistry = new DefaultCategorySemanticProfileRegistry(
                new DefaultCanonicalActionRegistry(registry),
                new DefaultCategorySemanticProfileSeedSource());
        CategorySemanticProfile profile = profileRegistry.getProfile(PromptCategory.WRITING).orElseThrow();
        List<ActionTypeInterface> expanded = expander.expand(
                PromptCategory.WRITING,
                ActionIntent.REWRITE,
                Set.copyOf(profile.getCompatibleActionGroupsForIntent(ActionIntent.REWRITE)),
                profile.getCompatibleActionsForIntent(ActionIntent.REWRITE)
        );
        List<ActionTypeInterface> orderedNoPref = policyNoPref.applyOrder(PromptCategory.WRITING, ActionIntent.REWRITE, expanded);
        if (orderedNoPref.size() < 2) return;

        String lastKey = orderedNoPref.get(orderedNoPref.size() - 1).key();
        ActionRecommendationPreferenceSource withPref = new DefaultActionRecommendationPreferenceSource(
                Map.of("WRITING+REWRITE", List.of(lastKey))
        );
        ActionRecommendationOrderPolicy policyWithPref = new DefaultActionRecommendationOrderPolicy(withPref);
        List<ActionTypeInterface> orderedWithPref = policyWithPref.applyOrder(PromptCategory.WRITING, ActionIntent.REWRITE, expanded);
        assertThat(orderedWithPref.get(0).key()).isEqualTo(lastKey);
    }

    @Test
    @DisplayName("Service orchestrates only: no expansion or sort logic in service")
    void serviceOrchestrationOnly() {
        ActionTypeRegistry registry = new ActionTypeRegistry(CATALOG);
        ConcreteActionByGroupIndex index = new DefaultConcreteActionByGroupIndex(registry.getAll());
        RecommendationConcreteActionExpander expander = new DefaultRecommendationConcreteActionExpander(index);
        PolicySourceRegistry policySourceRegistry = PolicyTestFixtures.registryWithSingleVersion(new DefaultActionRecommendationPreferenceSource(Map.of()));
        PolicySelectionStrategy strategy = PolicyTestFixtures.singleVersionStrategy();

        CanonicalActionRegistry canonical = new DefaultCanonicalActionRegistry(registry);
        SemanticRecommendationService service = new SemanticRecommendationService(canonical, expander, policySourceRegistry, strategy);
        DefaultCategorySemanticProfileRegistry profileRegistry = new DefaultCategorySemanticProfileRegistry(
                canonical, new DefaultCategorySemanticProfileSeedSource());
        CategorySemanticProfile profile = profileRegistry.getProfile(PromptCategory.WRITING).orElseThrow();

        RecommendationResult result = service.recommend(PromptCategory.WRITING, ActionIntent.GENERATE, profile, null, null, false);

        assertThat(result.actionCandidates()).isNotEmpty();
        PolicyVersion version = PolicyTestFixtures.testPolicyVersion();
        List<ActionTypeInterface> expectedOrder = policySourceRegistry.getActionRecommendationOrderPolicy(version).applyOrder(
                PromptCategory.WRITING,
                ActionIntent.GENERATE,
                expander.expand(
                        PromptCategory.WRITING,
                        ActionIntent.GENERATE,
                        Set.copyOf(profile.getCompatibleActionGroupsForIntent(ActionIntent.GENERATE)),
                        profile.getCompatibleActionsForIntent(ActionIntent.GENERATE)
                )
        );
        assertThat(result.actionCandidates()).containsExactlyElementsOf(expectedOrder);
        assertThat(result.recommendedAction()).contains(expectedOrder.get(0));
    }

    private static SemanticRecommendationService serviceWithPreference(
            Map<String, List<String>> preferredByContext,
            ConcreteActionByGroupIndex index,
            RecommendationConcreteActionExpander expander
    ) {
        ActionTypeRegistry registry = new ActionTypeRegistry(CATALOG);
        CanonicalActionRegistry canonical = new DefaultCanonicalActionRegistry(registry);
        PolicySourceRegistry policySourceRegistry = PolicyTestFixtures.registryWithSingleVersion(
                new DefaultActionRecommendationPreferenceSource(preferredByContext));
        PolicySelectionStrategy strategy = PolicyTestFixtures.singleVersionStrategy();
        return new SemanticRecommendationService(canonical, expander, policySourceRegistry, strategy);
    }
}
