package org.example.sharedprompts.domain.prompt.infrastructure.policy.diff;

import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyCompatibilityAnalyzer;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyDiffService;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.diff.PolicyChangeImpact;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.diff.PolicyChangeType;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.diff.PolicyDiff;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.diff.RuleChange;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.ValidatedPolicyBundle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Heuristic-based compatibility analyzer: classifies rule changes and derives affected scope.
 */
public final class DefaultPolicyCompatibilityAnalyzer implements PolicyCompatibilityAnalyzer {

    private static final Pattern CONTEXT_KEY = Pattern.compile("^([^+]+)\\+(.+)$");
    private static final String FAMILY_COMPATIBILITY = "Compatibility";
    private static final String FAMILY_RECOMMENDATION_PREFERENCE = "RecommendationPreference";
    private static final String FAMILY_ROLE_PREFERENCE = "RolePreference";
    private static final String FAMILY_ROLE_COMPATIBILITY = "RoleCompatibility";
    private static final String FAMILY_OBJECTIVE = "Objective";
    private static final String FAMILY_OBJECTIVE_DEFAULTS = "ObjectiveDomainDefaults";

    private final PolicyDiffService diffService;

    public DefaultPolicyCompatibilityAnalyzer(PolicyDiffService diffService) {
        this.diffService = diffService;
    }

    @Override
    public PolicyChangeImpact analyze(ValidatedPolicyBundle oldPolicy, ValidatedPolicyBundle newPolicy) {
        PolicyDiff diff = diffService.diff(oldPolicy, newPolicy);

        List<RuleChange> breaking = new ArrayList<>();
        List<RuleChange> safe = new ArrayList<>();
        List<RuleChange> behavior = new ArrayList<>();
        Set<String> categories = new HashSet<>();
        Set<String> intents = new HashSet<>();
        Set<String> actions = new HashSet<>();

        for (RuleChange c : diff.ruleChanges()) {
            PolicyChangeType type = classify(c);
            switch (type) {
                case BREAKING -> breaking.add(c);
                case SAFE -> safe.add(c);
                case BEHAVIOR_CHANGE -> behavior.add(c);
            }
            collectAffected(c, categories, intents, actions);
        }

        return new PolicyChangeImpact(breaking, safe, behavior, categories, intents, actions);
    }

    private PolicyChangeType classify(RuleChange c) {
        String family = c.policyFamily();
        switch (c.changeType()) {
            case ADD -> {
                if (FAMILY_COMPATIBILITY.equals(family)) return PolicyChangeType.SAFE;
                if (FAMILY_RECOMMENDATION_PREFERENCE.equals(family) || FAMILY_ROLE_PREFERENCE.equals(family)) return PolicyChangeType.SAFE;
                if (FAMILY_OBJECTIVE.equals(family) || FAMILY_OBJECTIVE_DEFAULTS.equals(family)) return PolicyChangeType.SAFE;
                if (FAMILY_ROLE_COMPATIBILITY.equals(family)) return PolicyChangeType.SAFE;
                return PolicyChangeType.SAFE;
            }
            case REMOVE -> {
                if (FAMILY_COMPATIBILITY.equals(family) || FAMILY_ROLE_COMPATIBILITY.equals(family)) return PolicyChangeType.BREAKING;
                if (FAMILY_RECOMMENDATION_PREFERENCE.equals(family) || FAMILY_ROLE_PREFERENCE.equals(family)) return PolicyChangeType.BREAKING;
                if (FAMILY_OBJECTIVE.equals(family) || FAMILY_OBJECTIVE_DEFAULTS.equals(family)) return PolicyChangeType.BREAKING;
                return PolicyChangeType.BREAKING;
            }
            case MODIFY -> {
                if (FAMILY_COMPATIBILITY.equals(family) || FAMILY_ROLE_COMPATIBILITY.equals(family)) return PolicyChangeType.BREAKING;
                if (FAMILY_RECOMMENDATION_PREFERENCE.equals(family) || FAMILY_ROLE_PREFERENCE.equals(family)) return PolicyChangeType.BEHAVIOR_CHANGE;
                if (FAMILY_OBJECTIVE.equals(family) || FAMILY_OBJECTIVE_DEFAULTS.equals(family)) return PolicyChangeType.BEHAVIOR_CHANGE;
                return PolicyChangeType.BEHAVIOR_CHANGE;
            }
            default -> { return PolicyChangeType.SAFE; }
        }
    }

    private void collectAffected(RuleChange c, Set<String> categories, Set<String> intents, Set<String> actions) {
        String id = c.ruleIdentifier();
        if (id == null || id.isBlank()) return;
        if (FAMILY_RECOMMENDATION_PREFERENCE.equals(c.policyFamily()) || FAMILY_COMPATIBILITY.equals(c.policyFamily())) {
            var m = CONTEXT_KEY.matcher(id);
            if (m.matches()) {
                categories.add(m.group(1));
                intents.add(m.group(2));
            }
            if (c.newValue() instanceof List<?> list) {
                for (Object o : list) if (o != null) actions.add(o.toString());
            }
            if (c.oldValue() instanceof List<?> list) {
                for (Object o : list) if (o != null) actions.add(o.toString());
            }
        }
        if (FAMILY_OBJECTIVE.equals(c.policyFamily())) {
            actions.add(id);
        }
    }
}
