package org.example.sharedprompts.domain.prompt.application.semantic.recommendation;

import org.example.sharedprompts.domain.prompt.common.enums.DeserializerEnumTestUtils;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.DefaultCanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfile;
import org.example.sharedprompts.domain.prompt.domain.semantic.RecommendationResult;
import org.example.sharedprompts.domain.prompt.domain.semantic.impl.DefaultCategorySemanticProfileRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.impl.DefaultCategorySemanticProfileSeedSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.*;
import org.example.sharedprompts.domain.prompt.domain.semantic.trace.RecommendationTrace;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.registry.PolicySourceRegistry;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicySelectionStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 8차: Internal trace is structurally produced; compatibility / expand / order / role phases separated;
 * fallback ordering leaves fallbackApplied in trace.
 */
@DisplayName("Semantic recommendation trace: structural trace, phase separation, fallback in trace")
class SemanticRecommendationServiceTraceTest {

    private static final List<Class<? extends Enum<?>>> CATALOG = DeserializerEnumTestUtils.getActionTypeEnums();

    @Test
    @DisplayName("Recommendation result carries structured trace when profile is present")
    void resultCarriesStructuredTrace() {
        ActionTypeRegistry registry = new ActionTypeRegistry(CATALOG);
        CanonicalActionRegistry canonical = new DefaultCanonicalActionRegistry(registry);
        ConcreteActionByGroupIndex index = new DefaultConcreteActionByGroupIndex(registry.getAll());
        RecommendationConcreteActionExpander expander = new DefaultRecommendationConcreteActionExpander(index);
        PolicySourceRegistry policyRegistry = PolicyTestFixtures.registryWithSingleVersion(new DefaultActionRecommendationPreferenceSource(Map.of()));
        PolicySelectionStrategy strategy = PolicyTestFixtures.singleVersionStrategy();
        SemanticRecommendationService service = new SemanticRecommendationService(canonical, expander, policyRegistry, strategy);
        DefaultCategorySemanticProfileRegistry profileRegistry = new DefaultCategorySemanticProfileRegistry(
                canonical, null, new DefaultCategorySemanticProfileSeedSource());
        CategorySemanticProfile profile = profileRegistry.getProfile(PromptCategory.WRITING).orElseThrow();

        RecommendationResult result = service.recommend(PromptCategory.WRITING, ActionIntent.GENERATE, profile, null, null, false);

        assertThat(result.trace()).isPresent();
        RecommendationTrace trace = result.trace().get();
        assertThat(trace.category()).isEqualTo(PromptCategory.WRITING);
        assertThat(trace.intent()).isEqualTo(ActionIntent.GENERATE);
        assertThat(trace.compatibilityTrace()).isPresent();
        assertThat(trace.expansionTrace()).isPresent();
        assertThat(trace.actionOrderingTrace()).isPresent();
        assertThat(trace.actionTraces()).isNotEmpty();
        assertThat(trace.actionTraces().size()).isEqualTo(result.actionCandidates().size());
        assertThat(trace.roleTraces()).isNotEmpty();
    }

    @Test
    @DisplayName("When no preference source, ordering trace has fallbackOrderingApplied true")
    void fallbackOrderingRecordedInTrace() {
        ActionTypeRegistry registry = new ActionTypeRegistry(CATALOG);
        CanonicalActionRegistry canonical = new DefaultCanonicalActionRegistry(registry);
        ConcreteActionByGroupIndex index = new DefaultConcreteActionByGroupIndex(registry.getAll());
        RecommendationConcreteActionExpander expander = new DefaultRecommendationConcreteActionExpander(index);
        PolicySourceRegistry policyRegistry = PolicyTestFixtures.registryWithSingleVersion(new DefaultActionRecommendationPreferenceSource(Map.of()));
        PolicySelectionStrategy strategy = PolicyTestFixtures.singleVersionStrategy();
        SemanticRecommendationService service = new SemanticRecommendationService(canonical, expander, policyRegistry, strategy);
        DefaultCategorySemanticProfileRegistry profileRegistry = new DefaultCategorySemanticProfileRegistry(
                canonical, null, new DefaultCategorySemanticProfileSeedSource());
        CategorySemanticProfile profile = profileRegistry.getProfile(PromptCategory.WRITING).orElseThrow();

        RecommendationResult result = service.recommend(PromptCategory.WRITING, ActionIntent.GENERATE, profile, null, null, false);

        assertThat(result.trace()).isPresent();
        assertThat(result.trace().get().actionOrderingTrace()).isPresent();
        assertThat(result.trace().get().actionOrderingTrace().get().fallbackOrderingApplied()).isTrue();
    }

    @Test
    @DisplayName("Preference source change yields different ordering; trace still present")
    void preferenceChangeTraceStillPresent() {
        ActionTypeRegistry registry = new ActionTypeRegistry(CATALOG);
        CanonicalActionRegistry canonical = new DefaultCanonicalActionRegistry(registry);
        ConcreteActionByGroupIndex index = new DefaultConcreteActionByGroupIndex(registry.getAll());
        RecommendationConcreteActionExpander expander = new DefaultRecommendationConcreteActionExpander(index);
        DefaultCategorySemanticProfileRegistry profileRegistry = new DefaultCategorySemanticProfileRegistry(
                canonical, null, new DefaultCategorySemanticProfileSeedSource());
        CategorySemanticProfile profile = profileRegistry.getProfile(PromptCategory.WRITING).orElseThrow();

        PolicySourceRegistry registryEmpty = PolicyTestFixtures.registryWithSingleVersion(new DefaultActionRecommendationPreferenceSource(Map.of()));
        PolicySelectionStrategy strategy = PolicyTestFixtures.singleVersionStrategy();
        SemanticRecommendationService serviceEmpty = new SemanticRecommendationService(canonical, expander, registryEmpty, strategy);
        RecommendationResult resultEmpty = serviceEmpty.recommend(PromptCategory.WRITING, ActionIntent.GENERATE, profile, null, null, false);

        Map<String, List<String>> customPref = Map.of("WRITING+GENERATE", List.of("ACTION.WRITING.ESSAY_WRITING", "ACTION.WRITING.ARTICLE_WRITING"));
        PolicySourceRegistry registryWithPref = PolicyTestFixtures.registryWithSingleVersion(new DefaultActionRecommendationPreferenceSource(customPref));
        SemanticRecommendationService serviceWithPref = new SemanticRecommendationService(canonical, expander, registryWithPref, strategy);
        RecommendationResult resultWithPref = serviceWithPref.recommend(PromptCategory.WRITING, ActionIntent.GENERATE, profile, null, null, false);

        assertThat(resultEmpty.trace()).isPresent();
        assertThat(resultWithPref.trace()).isPresent();
        assertThat(resultEmpty.actionCandidates().stream().map(ActionTypeInterface::key).toList())
                .isNotEqualTo(resultWithPref.actionCandidates().stream().map(ActionTypeInterface::key).toList());
    }

    @Test
    @DisplayName("No profile returns empty trace")
    void noProfileReturnsEmptyTrace() {
        ActionTypeRegistry registry = new ActionTypeRegistry(CATALOG);
        CanonicalActionRegistry canonical = new DefaultCanonicalActionRegistry(registry);
        ConcreteActionByGroupIndex index = new DefaultConcreteActionByGroupIndex(registry.getAll());
        RecommendationConcreteActionExpander expander = new DefaultRecommendationConcreteActionExpander(index);
        PolicySourceRegistry policyRegistry = PolicyTestFixtures.registryWithSingleVersion(new DefaultActionRecommendationPreferenceSource(Map.of()));
        PolicySelectionStrategy strategy = PolicyTestFixtures.singleVersionStrategy();
        SemanticRecommendationService service = new SemanticRecommendationService(canonical, expander, policyRegistry, strategy);

        RecommendationResult result = service.recommend(PromptCategory.WRITING, ActionIntent.GENERATE, null, null, null, false);

        assertThat(result.trace()).isEmpty();
    }

    @Test
    @DisplayName("Policy version is recorded in trace")
    void policyVersionRecordedInTrace() {
        ActionTypeRegistry registry = new ActionTypeRegistry(CATALOG);
        CanonicalActionRegistry canonical = new DefaultCanonicalActionRegistry(registry);
        ConcreteActionByGroupIndex index = new DefaultConcreteActionByGroupIndex(registry.getAll());
        RecommendationConcreteActionExpander expander = new DefaultRecommendationConcreteActionExpander(index);
        PolicySourceRegistry policySourceRegistry = PolicyTestFixtures.registryWithSingleVersion(new DefaultActionRecommendationPreferenceSource(Map.of()));
        PolicySelectionStrategy strategy = PolicyTestFixtures.singleVersionStrategy();
        SemanticRecommendationService service = new SemanticRecommendationService(canonical, expander, policySourceRegistry, strategy);
        DefaultCategorySemanticProfileRegistry profileRegistry = new DefaultCategorySemanticProfileRegistry(
                canonical, null, new DefaultCategorySemanticProfileSeedSource());
        CategorySemanticProfile profile = profileRegistry.getProfile(PromptCategory.WRITING).orElseThrow();

        RecommendationResult result = service.recommend(PromptCategory.WRITING, ActionIntent.GENERATE, profile, null, null, false);

        assertThat(result.trace()).isPresent();
        assertThat(result.trace().get().policyTraceInfo()).isPresent();
        assertThat(result.trace().get().policyTraceInfo().get().policyVersionId()).isEqualTo(PolicyTestFixtures.TEST_POLICY_VERSION_ID);
        assertThat(result.trace().get().policyTraceInfo().get().policySourceId()).isNotNull();
    }
}
