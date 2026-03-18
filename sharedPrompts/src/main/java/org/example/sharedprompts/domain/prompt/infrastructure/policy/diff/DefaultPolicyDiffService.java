package org.example.sharedprompts.domain.prompt.infrastructure.policy.diff;

import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyDiffService;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.diff.PolicyDiff;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.diff.RuleChange;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.*;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.ValidatedPolicyBundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Document-based policy diff using stable rule identifiers (map keys).
 */
public final class DefaultPolicyDiffService implements PolicyDiffService {

    private static final String FAMILY_RECOMMENDATION_PREFERENCE = "RecommendationPreference";
    private static final String FAMILY_COMPATIBILITY = "Compatibility";
    private static final String FAMILY_OBJECTIVE = "Objective";
    private static final String FAMILY_OBJECTIVE_DEFAULTS = "ObjectiveDomainDefaults";
    private static final String FAMILY_ROLE_PREFERENCE = "RolePreference";
    private static final String FAMILY_ROLE_COMPATIBILITY = "RoleCompatibility";

    @Override
    public PolicyDiff diff(ValidatedPolicyBundle from, ValidatedPolicyBundle to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required");
        }
        String fromVersion = from.policyVersion() != null ? from.policyVersion().versionId() : "";
        String toVersion = to.policyVersion() != null ? to.policyVersion().versionId() : "";

        List<RuleChange> changes = new ArrayList<>();
        diffRuleMaps(changes, FAMILY_RECOMMENDATION_PREFERENCE, rules(from.recommendationPreference()), rules(to.recommendationPreference()));
        diffRuleMaps(changes, FAMILY_COMPATIBILITY, rules(from.compatibility()), rules(to.compatibility()));
        diffObjective(changes, from.objective(), to.objective());
        diffRuleMaps(changes, FAMILY_ROLE_PREFERENCE, rules(from.rolePreference()), rules(to.rolePreference()));
        diffRuleMaps(changes, FAMILY_ROLE_COMPATIBILITY, rules(from.roleCompatibility()), rules(to.roleCompatibility()));

        String summary = buildSummary(fromVersion, toVersion, changes);
        return new PolicyDiff(fromVersion, toVersion, summary, changes);
    }

    private static Map<String, List<String>> rules(Optional<? extends PolicyDocument> doc) {
        if (doc.isEmpty()) return Map.of();
        PolicyDocument d = doc.get();
        if (d instanceof RecommendationPreferencePolicyDocument r) return r.rules();
        if (d instanceof CompatibilityPolicyDocument c) return c.rules();
        if (d instanceof RolePreferencePolicyDocument r) return r.rules();
        if (d instanceof RoleCompatibilityPolicyDocument r) return r.rules();
        return Map.of();
    }

    private void diffRuleMaps(
            List<RuleChange> out,
            String policyFamily,
            Map<String, List<String>> fromRules,
            Map<String, List<String>> toRules
    ) {
        Set<String> allKeys = new TreeSet<>();
        allKeys.addAll(fromRules != null ? fromRules.keySet() : Set.of());
        allKeys.addAll(toRules != null ? toRules.keySet() : Set.of());
        for (String key : allKeys) {
            List<String> fromVal = fromRules != null && fromRules.containsKey(key) ? fromRules.get(key) : null;
            List<String> toVal = toRules != null && toRules.containsKey(key) ? toRules.get(key) : null;
            if (fromVal == null && toVal != null) {
                out.add(RuleChange.add(policyFamily, key, toVal));
            } else if (fromVal != null && toVal == null) {
                out.add(RuleChange.remove(policyFamily, key, fromVal));
            } else if (fromVal != null && toVal != null && !Objects.equals(fromVal, toVal)) {
                out.add(RuleChange.modify(policyFamily, key, fromVal, toVal));
            }
        }
    }

    private void diffObjective(
            List<RuleChange> out,
            Optional<ObjectivePolicyDocument> fromOpt,
            Optional<ObjectivePolicyDocument> toOpt
    ) {
        Map<String, String> fromExplicit = fromOpt.map(ObjectivePolicyDocument::explicitMappings).orElse(Map.of());
        Map<String, String> toExplicit = toOpt.map(ObjectivePolicyDocument::explicitMappings).orElse(Map.of());
        diffStringMaps(out, FAMILY_OBJECTIVE, fromExplicit, toExplicit);
        Map<String, String> fromDef = fromOpt.map(ObjectivePolicyDocument::domainDefaults).orElse(Map.of());
        Map<String, String> toDef = toOpt.map(ObjectivePolicyDocument::domainDefaults).orElse(Map.of());
        diffStringMaps(out, FAMILY_OBJECTIVE_DEFAULTS, fromDef, toDef);
    }

    private void diffStringMaps(
            List<RuleChange> out,
            String policyFamily,
            Map<String, String> fromMap,
            Map<String, String> toMap
    ) {
        Set<String> allKeys = new TreeSet<>();
        allKeys.addAll(fromMap != null ? fromMap.keySet() : Set.of());
        allKeys.addAll(toMap != null ? toMap.keySet() : Set.of());
        for (String key : allKeys) {
            String fromVal = fromMap != null && fromMap.containsKey(key) ? fromMap.get(key) : null;
            String toVal = toMap != null && toMap.containsKey(key) ? toMap.get(key) : null;
            if (fromVal == null && toVal != null) {
                out.add(RuleChange.add(policyFamily, key, toVal));
            } else if (fromVal != null && toVal == null) {
                out.add(RuleChange.remove(policyFamily, key, fromVal));
            } else if (fromVal != null && toVal != null && !Objects.equals(fromVal, toVal)) {
                out.add(RuleChange.modify(policyFamily, key, fromVal, toVal));
            }
        }
    }

    private static String buildSummary(String fromVersion, String toVersion, List<RuleChange> changes) {
        long add = changes.stream().filter(c -> c.changeType() == RuleChange.RuleChangeType.ADD).count();
        long remove = changes.stream().filter(c -> c.changeType() == RuleChange.RuleChangeType.REMOVE).count();
        long modify = changes.stream().filter(c -> c.changeType() == RuleChange.RuleChangeType.MODIFY).count();
        return String.format("%s → %s: +%d -%d ~%d", fromVersion, toVersion, add, remove, modify);
    }
}
