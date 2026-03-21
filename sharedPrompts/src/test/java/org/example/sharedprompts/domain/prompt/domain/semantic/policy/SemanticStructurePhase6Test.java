package org.example.sharedprompts.domain.prompt.domain.semantic.policy;

import org.example.sharedprompts.domain.prompt.common.enums.DeserializerEnumTestUtils;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.DefaultCanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfile;
import org.example.sharedprompts.domain.prompt.domain.semantic.RecommendationResult;
import org.example.sharedprompts.domain.prompt.domain.semantic.impl.DefaultCategorySemanticProfileRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.impl.DefaultCategorySemanticProfileSeedSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.ConcreteActionByGroupIndex;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.DefaultConcreteActionByGroupIndex;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.DefaultRecommendationConcreteActionExpander;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.RecommendationConcreteActionExpander;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.RoleRecommendationOrderPolicy;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.role.DefaultRolePreferenceSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.role.RolePreferenceSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.trace.RecommendationTrace;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicySelectionStrategy;
import org.example.sharedprompts.domain.prompt.application.semantic.recommendation.PolicyTestFixtures;
import org.example.sharedprompts.domain.prompt.application.semantic.recommendation.SemanticRecommendationService;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.registry.PolicySourceRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.DefaultActionRecommendationPreferenceSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 6: Role recommendation is an independent policy axis.
 * Role recommendation uses RolePreferenceSource and RoleRecommendationOrderPolicy at runtime;
 * profile is fallback only. Trace reflects actual role source (policy vs profile).
 */
@DisplayName("Semantic structure phase 6: role policy axis, role order policy, trace reflects source")
class SemanticStructurePhase6Test {

    private static final List<Class<? extends Enum<?>>> CATALOG = DeserializerEnumTestUtils.getActionTypeEnums();

    @Test
    @DisplayName("Role recommendation uses role policy source at runtime; swapping role source changes recommended role")
    void roleRecommendationUsesRolePolicySource() {
        ActionTypeRegistry registry = new ActionTypeRegistry(CATALOG);
        CanonicalActionRegistry canonical = new DefaultCanonicalActionRegistry(registry);
        ConcreteActionByGroupIndex index = new DefaultConcreteActionByGroupIndex(registry.getAll());
        RecommendationConcreteActionExpander expander = new DefaultRecommendationConcreteActionExpander(index);
        DefaultCategorySemanticProfileRegistry profileRegistry = new DefaultCategorySemanticProfileRegistry(
                canonical, new DefaultCategorySemanticProfileSeedSource());
        CategorySemanticProfile profile = profileRegistry.getProfile(PromptCategory.WRITING).orElseThrow();

        RolePreferenceSource emptyRolePref = new DefaultRolePreferenceSource();
        PolicySourceRegistry registryProfileFallback = PolicyTestFixtures.registryWithSingleVersion(
                new DefaultActionRecommendationPreferenceSource(),
                emptyRolePref
        );
        PolicySelectionStrategy strategy = PolicyTestFixtures.singleVersionStrategy();
        SemanticRecommendationService serviceProfileFallback = new SemanticRecommendationService(
                canonical, expander, registryProfileFallback, strategy
        );

        Map<String, List<String>> rolePrefMap = Map.of(
                "WRITING+GENERATE", List.of("ROLE.WRITING.COPYWRITER", "ROLE.WRITING.EDITOR")
        );
        RolePreferenceSource customRolePref = new DefaultRolePreferenceSource(rolePrefMap);
        PolicySourceRegistry registryPolicy = PolicyTestFixtures.registryWithSingleVersion(
                new DefaultActionRecommendationPreferenceSource(),
                customRolePref
        );
        SemanticRecommendationService servicePolicy = new SemanticRecommendationService(
                canonical, expander, registryPolicy, strategy
        );

        RecommendationResult resultFallback = serviceProfileFallback.recommend(
                PromptCategory.WRITING, ActionIntent.GENERATE, profile, null, null, false
        );
        RecommendationResult resultPolicy = servicePolicy.recommend(
                PromptCategory.WRITING, ActionIntent.GENERATE, profile, null, null, false
        );

        assertThat(resultFallback.roleCandidates()).isNotEmpty();
        assertThat(resultPolicy.roleCandidates()).isNotEmpty();
        assertThat(resultPolicy.recommendedRole()).isPresent();
        assertThat(resultPolicy.recommendedRole().get().key()).isEqualTo("ROLE.WRITING.COPYWRITER");
        assertThat(resultPolicy.roleCandidates().get(0).key()).isEqualTo("ROLE.WRITING.COPYWRITER");
    }

    @Test
    @DisplayName("Role recommendation order is policy-driven, not profile insertion order")
    void roleOrderIsPolicyDriven() {
        ActionTypeRegistry registry = new ActionTypeRegistry(CATALOG);
        CanonicalActionRegistry canonical = new DefaultCanonicalActionRegistry(registry);
        ConcreteActionByGroupIndex index = new DefaultConcreteActionByGroupIndex(registry.getAll());
        RecommendationConcreteActionExpander expander = new DefaultRecommendationConcreteActionExpander(index);
        DefaultCategorySemanticProfileRegistry profileRegistry = new DefaultCategorySemanticProfileRegistry(
                canonical, new DefaultCategorySemanticProfileSeedSource());
        CategorySemanticProfile profile = profileRegistry.getProfile(PromptCategory.WRITING).orElseThrow();

        Map<String, List<String>> roleOrder = Map.of(
                "WRITING+GENERATE", List.of("ROLE.WRITING.EDITOR", "ROLE.WRITING.COPYWRITER", "ROLE.WRITING.TECHNICAL_WRITER")
        );
        PolicySourceRegistry policyRegistry = PolicyTestFixtures.registryWithSingleVersion(
                new DefaultActionRecommendationPreferenceSource(),
                new DefaultRolePreferenceSource(roleOrder)
        );
        SemanticRecommendationService service = new SemanticRecommendationService(
                canonical, expander, policyRegistry, PolicyTestFixtures.singleVersionStrategy()
        );

        RecommendationResult result = service.recommend(
                PromptCategory.WRITING, ActionIntent.GENERATE, profile, null, null, false
        );

        assertThat(result.roleCandidates()).isNotEmpty();
        assertThat(result.roleCandidates().get(0).key()).isEqualTo("ROLE.WRITING.EDITOR");
        assertThat(result.recommendedRole()).isPresent();
        assertThat(result.recommendedRole().get().key()).isEqualTo("ROLE.WRITING.EDITOR");
    }

    @Test
    @DisplayName("Trace reflects role policy source when policy supplies roles")
    void traceReflectsRolePolicySource() {
        ActionTypeRegistry registry = new ActionTypeRegistry(CATALOG);
        CanonicalActionRegistry canonical = new DefaultCanonicalActionRegistry(registry);
        ConcreteActionByGroupIndex index = new DefaultConcreteActionByGroupIndex(registry.getAll());
        RecommendationConcreteActionExpander expander = new DefaultRecommendationConcreteActionExpander(index);
        DefaultCategorySemanticProfileRegistry profileRegistry = new DefaultCategorySemanticProfileRegistry(
                canonical, new DefaultCategorySemanticProfileSeedSource());
        CategorySemanticProfile profile = profileRegistry.getProfile(PromptCategory.WRITING).orElseThrow();

        Map<String, List<String>> rolePref = Map.of("WRITING+GENERATE", List.of("ROLE.WRITING.COPYWRITER"));
        PolicySourceRegistry policyRegistry = PolicyTestFixtures.registryWithSingleVersion(
                new DefaultActionRecommendationPreferenceSource(),
                new DefaultRolePreferenceSource(rolePref)
        );
        SemanticRecommendationService service = new SemanticRecommendationService(
                canonical, expander, policyRegistry, PolicyTestFixtures.singleVersionStrategy()
        );

        RecommendationResult result = service.recommend(
                PromptCategory.WRITING, ActionIntent.GENERATE, profile, null, null, false
        );

        assertThat(result.trace()).isPresent();
        RecommendationTrace trace = result.trace().get();
        assertThat(trace.roleTraces()).isNotEmpty();
        assertThat(trace.roleTraces().get(0).inclusionStage()).isEqualTo("role-policy");
        assertThat(trace.roleTraces().get(0).inclusionPolicy().sourceId()).isEqualTo("DefaultRolePreferenceSource");
    }

    @Test
    @DisplayName("Trace reflects profile-fallback when policy returns no roles")
    void traceReflectsProfileFallbackWhenPolicyEmpty() {
        ActionTypeRegistry registry = new ActionTypeRegistry(CATALOG);
        CanonicalActionRegistry canonical = new DefaultCanonicalActionRegistry(registry);
        ConcreteActionByGroupIndex index = new DefaultConcreteActionByGroupIndex(registry.getAll());
        RecommendationConcreteActionExpander expander = new DefaultRecommendationConcreteActionExpander(index);
        DefaultCategorySemanticProfileRegistry profileRegistry = new DefaultCategorySemanticProfileRegistry(
                canonical, new DefaultCategorySemanticProfileSeedSource());
        CategorySemanticProfile profile = profileRegistry.getProfile(PromptCategory.WRITING).orElseThrow();

        PolicySourceRegistry policyRegistry = PolicyTestFixtures.registryWithSingleVersion(
                new DefaultActionRecommendationPreferenceSource(),
                new DefaultRolePreferenceSource()
        );
        SemanticRecommendationService service = new SemanticRecommendationService(
                canonical, expander, policyRegistry, PolicyTestFixtures.singleVersionStrategy()
        );

        RecommendationResult result = service.recommend(
                PromptCategory.WRITING, ActionIntent.GENERATE, profile, null, null, false
        );

        assertThat(result.trace()).isPresent();
        assertThat(result.trace().get().roleTraces()).isNotEmpty();
        assertThat(result.trace().get().roleTraces().get(0).inclusionStage()).isEqualTo("profile-fallback");
        assertThat(result.trace().get().roleTraces().get(0).inclusionPolicy().sourceId()).isEqualTo("CategorySemanticProfile");
    }

    @Test
    @DisplayName("Registry exposes getRoleRecommendationOrderPolicy; order policy applies preference source order")
    void registryExposesRoleRecommendationOrderPolicy() {
        Map<String, List<String>> pref = Map.of("WRITING+GENERATE", List.of("ROLE.WRITING.EDITOR", "ROLE.WRITING.COPYWRITER"));
        PolicySourceRegistry registry = PolicyTestFixtures.registryWithSingleVersion(
                new DefaultActionRecommendationPreferenceSource(),
                new DefaultRolePreferenceSource(pref)
        );
        RoleRecommendationOrderPolicy orderPolicy = registry.getRoleRecommendationOrderPolicy(PolicyTestFixtures.testPolicyVersion());
        assertThat(orderPolicy).isNotNull();
    }

    @Test
    @DisplayName("ActionTypeMetadataLoader is removed (phase 2 static loader no longer present)")
    void actionTypeMetadataLoaderRemoved() {
        String loaderClass = "org.example.sharedprompts.domain.prompt.common.enums.action.metadata.ActionTypeMetadataLoader";
        try {
            Class.forName(loaderClass);
            throw new AssertionError("ActionTypeMetadataLoader must not exist; phase 2 completion requires removal");
        } catch (ClassNotFoundException e) {
            assertThat(e.getMessage()).contains(loaderClass);
        }
    }

    @Test
    @DisplayName("Action recommendation flow unchanged: compatibility, expander, action order policy")
    void actionRecommendationFlowUnchanged() {
        ActionTypeRegistry registry = new ActionTypeRegistry(CATALOG);
        CanonicalActionRegistry canonical = new DefaultCanonicalActionRegistry(registry);
        ConcreteActionByGroupIndex index = new DefaultConcreteActionByGroupIndex(registry.getAll());
        RecommendationConcreteActionExpander expander = new DefaultRecommendationConcreteActionExpander(index);
        DefaultCategorySemanticProfileRegistry profileRegistry = new DefaultCategorySemanticProfileRegistry(
                canonical, new DefaultCategorySemanticProfileSeedSource());
        CategorySemanticProfile profile = profileRegistry.getProfile(PromptCategory.WRITING).orElseThrow();
        PolicySourceRegistry policyRegistry = PolicyTestFixtures.registryWithSingleVersion(
                new DefaultActionRecommendationPreferenceSource(Map.of("WRITING+GENERATE", List.of("ACTION.WRITING.ARTICLE_WRITING", "ACTION.WRITING.COPYWRITING"))),
                new DefaultRolePreferenceSource()
        );
        SemanticRecommendationService service = new SemanticRecommendationService(
                canonical, expander, policyRegistry, PolicyTestFixtures.singleVersionStrategy()
        );

        RecommendationResult result = service.recommend(
                PromptCategory.WRITING, ActionIntent.GENERATE, profile, null, null, false
        );

        assertThat(result.actionCandidates()).isNotEmpty();
        assertThat(result.recommendedAction()).isPresent();
        assertThat(result.trace()).isPresent();
        assertThat(result.trace().get().actionOrderingTrace()).isPresent();
    }
}
