package org.example.sharedprompts.domain.prompt.application.semantic.recommendation;

import org.example.sharedprompts.domain.prompt.application.semantic.experiment.ExperimentContext;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicySelectionStrategy;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfile;
import org.example.sharedprompts.domain.prompt.domain.semantic.RecommendationResult;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeResolver;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.registry.PolicySourceRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.ActionRecommendationOrderPolicy;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.RecommendationConcreteActionExpander;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.RoleRecommendationOrderPolicy;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.role.RolePreferenceSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.version.PolicyVersion;
import org.example.sharedprompts.domain.prompt.domain.semantic.trace.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Orchestrates recommendation: profile (compatibility) → expander (concrete candidates) → order policy (order).
 * Policy version is selected via PolicySelectionStrategy; sources come from PolicySourceRegistry.
 * Collects structured trace including policy version/source for audit.
 */
@Service
public class SemanticRecommendationService {

    private static final String COMPATIBILITY_SOURCE_KIND = "profile";
    private static final String COMPATIBILITY_SOURCE_ID = "CategorySemanticProfile";
    private static final String EXPANSION_SOURCE_ID = "DefaultRecommendationConcreteActionExpander";
    private static final String ROLE_POLICY_SOURCE_KIND = "in-memory";
    private static final String ROLE_POLICY_SOURCE_ID = "DefaultRolePreferenceSource";
    private static final String ROLE_PROFILE_FALLBACK_SOURCE_ID = "CategorySemanticProfile";

    private final CanonicalActionRegistry canonicalActionRegistry;
    private final RecommendationConcreteActionExpander concreteActionExpander;
    private final PolicySourceRegistry policySourceRegistry;
    private final PolicySelectionStrategy policySelectionStrategy;

    public SemanticRecommendationService(
            CanonicalActionRegistry canonicalActionRegistry,
            RecommendationConcreteActionExpander concreteActionExpander,
            PolicySourceRegistry policySourceRegistry,
            PolicySelectionStrategy policySelectionStrategy
    ) {
        this.canonicalActionRegistry = canonicalActionRegistry;
        this.concreteActionExpander = concreteActionExpander;
        this.policySourceRegistry = policySourceRegistry;
        this.policySelectionStrategy = policySelectionStrategy;
    }

    public RecommendationResult recommend(
            PromptCategory category,
            ActionIntent intent,
            CategorySemanticProfile profile,
            RoleTypeInterface userProvidedRole,
            ActionTypeInterface userProvidedAction,
            boolean fallbackIntentWasUsed
    ) {
        return recommend(category, intent, profile, userProvidedRole, userProvidedAction, fallbackIntentWasUsed, null);
    }

    public RecommendationResult recommend(
            PromptCategory category,
            ActionIntent intent,
            CategorySemanticProfile profile,
            RoleTypeInterface userProvidedRole,
            ActionTypeInterface userProvidedAction,
            boolean fallbackIntentWasUsed,
            ExperimentContext experimentContext
    ) {
        if (profile == null || intent == null) {
            return new RecommendationResult(
                    category,
                    intent,
                    Optional.empty(),
                    Optional.empty(),
                    List.of(),
                    List.of(),
                    List.of("No profile or intent for recommendation."),
                    Optional.empty()
            );
        }

        ExperimentContext ctx = experimentContext != null ? experimentContext : ExperimentContext.empty();
        PolicyVersion version = policySelectionStrategy.selectPolicyVersion(ctx);
        ActionRecommendationOrderPolicy orderPolicy = policySourceRegistry.getActionRecommendationOrderPolicy(version);

        DefaultRecommendationTraceCollector collector = new DefaultRecommendationTraceCollector(category, intent, fallbackIntentWasUsed);
        collector.setPolicyTraceInfo(PolicyTraceInfo.of(
                version.versionId(),
                version.sourceType(),
                null,
                null
        ));

        RoleRecommendationOrderPolicy roleOrderPolicy = policySourceRegistry.getRoleRecommendationOrderPolicy(version);
        RolePreferenceSource rolePreferenceSource = policySourceRegistry.getRolePreferenceSource(version);
        List<RoleTypeInterface> fromPolicy = roleCandidatesFromPolicy(category, intent, rolePreferenceSource);
        boolean roleSourceFromPolicy = !fromPolicy.isEmpty();
        List<RoleTypeInterface> roleCandidatesRaw = fromPolicy.isEmpty() && profile != null
                ? profile.getRecommendedRolesForIntent(intent)
                : fromPolicy;
        List<RoleTypeInterface> roleCandidates = roleOrderPolicy.applyOrder(category, intent, roleCandidatesRaw, collector);

        Set<ActionGroup> allowedGroups = new LinkedHashSet<>(profile.getCompatibleActionGroupsForIntent(intent));
        List<ActionTypeInterface> seedActions = profile.getCompatibleActionsForIntent(intent);

        List<String> allowedGroupNames = allowedGroups.stream().map(Enum::name).collect(Collectors.toList());
        List<String> allowedActionKeys = seedActions.stream().map(ActionTypeInterface::key).collect(Collectors.toList());
        PolicyApplicationTrace compatibilityPolicy = PolicyApplicationTrace.of(COMPATIBILITY_SOURCE_KIND, COMPATIBILITY_SOURCE_ID, "Compatibility", false);
        collector.recordCompatibility(allowedGroupNames, allowedActionKeys, compatibilityPolicy);

        List<ActionTypeInterface> expanded = concreteActionExpander.expand(category, intent, allowedGroups, seedActions);
        List<String> expandedKeys = expanded.stream().map(ActionTypeInterface::key).collect(Collectors.toList());
        PolicyApplicationTrace expansionPolicy = PolicyApplicationTrace.of("in-memory", EXPANSION_SOURCE_ID, "Expansion", false);
        collector.recordExpansion(expandedKeys, expansionPolicy);

        List<ActionTypeInterface> actionCandidates = orderPolicy.applyOrder(category, intent, expanded, collector);

        PolicyApplicationTrace inclusionPolicy = PolicyApplicationTrace.of(COMPATIBILITY_SOURCE_KIND, COMPATIBILITY_SOURCE_ID, "Compatibility+Expansion", false);
        List<ActionRecommendationTrace> actionTraces = actionCandidates.stream()
                .map(a -> ActionRecommendationTrace.of(a.key(), "compatibility+expansion", inclusionPolicy))
                .collect(Collectors.toList());
        collector.setActionTraces(actionTraces);

        PolicyApplicationTrace roleInclusionPolicy = roleSourceFromPolicy
                ? PolicyApplicationTrace.of(ROLE_POLICY_SOURCE_KIND, ROLE_POLICY_SOURCE_ID, "RoleRecommendationOrder", false)
                : PolicyApplicationTrace.of(COMPATIBILITY_SOURCE_KIND, ROLE_PROFILE_FALLBACK_SOURCE_ID, "RoleCompatibility", false);
        String roleInclusionStage = roleSourceFromPolicy ? "role-policy" : "profile-fallback";
        List<RoleRecommendationTrace> roleTraces = roleCandidates.stream()
                .map(r -> RoleRecommendationTrace.of(r.key(), roleInclusionStage, roleInclusionPolicy))
                .collect(Collectors.toList());
        collector.setRoleTraces(roleTraces);

        RecommendationTrace trace = collector.build();

        Optional<RoleTypeInterface> recommendedRole = Optional.empty();
        if (userProvidedRole != null) {
            recommendedRole = Optional.of(userProvidedRole);
        } else if (!roleCandidates.isEmpty()) {
            recommendedRole = Optional.of(roleCandidates.get(0));
        }

        Optional<ActionTypeInterface> recommendedAction = Optional.empty();
        if (userProvidedAction != null) {
            recommendedAction = Optional.of(userProvidedAction);
        } else if (!actionCandidates.isEmpty()) {
            recommendedAction = Optional.of(actionCandidates.get(0));
        }

        return new RecommendationResult(
                category,
                intent,
                recommendedRole,
                recommendedAction,
                roleCandidates,
                actionCandidates,
                List.of(),
                Optional.of(trace)
        );
    }

    public RecommendationResult recommend(
            PromptCategory category,
            ActionIntent intent,
            CategorySemanticProfile profile,
            RoleTypeInterface userProvidedRole,
            ActionTypeInterface userProvidedAction
    ) {
        return recommend(category, intent, profile, userProvidedRole, userProvidedAction, false);
    }

    /** Build role candidates from policy preferred keys; preserves policy order. */
    private static List<RoleTypeInterface> roleCandidatesFromPolicy(
            PromptCategory category,
            ActionIntent intent,
            RolePreferenceSource rolePreferenceSource
    ) {
        List<String> keys = rolePreferenceSource.getPreferredRoleKeys(category, intent);
        if (keys == null || keys.isEmpty()) return List.of();
        List<RoleTypeInterface> out = new ArrayList<>();
        for (String key : keys) {
            RoleTypeResolver.resolveOptional(key).ifPresent(out::add);
        }
        return out;
    }
}
