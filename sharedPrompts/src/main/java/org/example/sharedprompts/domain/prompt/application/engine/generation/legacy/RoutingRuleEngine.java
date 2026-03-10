package org.example.sharedprompts.domain.prompt.application.engine.generation.legacy;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/** 룰 기반 라우팅 override 엔진 */
@Slf4j
@Component
public class RoutingRuleEngine {

    private final List<RoutingRule> rules = new CopyOnWriteArrayList<>();

    public RoutingRuleEngine() {
    }

    public RoutingRuleEngine(List<RoutingRule> initialRules) {
        if (initialRules != null) {
            initialRules.forEach(this::registerRule);
        }
    }

    public void registerRule(RoutingRule rule) {
        Objects.requireNonNull(rule, "rule must not be null");
        if (rule.getId() == null || rule.getId().isBlank()) {
            throw new IllegalArgumentException("rule.id must not be blank");
        }
        Objects.requireNonNull(rule.getCondition(), "rule.condition must not be null");
        boolean duplicateExists = rules.stream().anyMatch(r -> r.getId().equals(rule.getId()));
        if (duplicateExists) {
            throw new IllegalArgumentException("duplicate rule.id: " + rule.getId());
        }
        rules.add(rule);
    }

    public RoutingOverrides apply(UnifiedGeneratePromptCommand command, IntentDefaults defaults) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(defaults, "defaults must not be null");
        if (rules.isEmpty()) {
            return RoutingOverrides.empty();
        }

        List<MatchedRule> matched = collectMatchedRules(command, defaults);
        if (matched.isEmpty()) {
            return RoutingOverrides.empty();
        }

        matched.sort(byPriorityThenSpecificityThenOrder());
        RoutingRule winner = matched.get(0).rule;
        List<String> appliedRuleIds = matched.stream().map(m -> m.rule.getId()).toList();

        log.debug("[UnifiedRouting][RuleEngine] matchedRules={}, winner={}", appliedRuleIds, winner.getId());

        return new RoutingOverrides(
                winner.getOverrideObjective().orElse(null),
                winner.getOverrideOutputNeeds().orElse(null),
                winner.getOverrideResponseShape().orElse(null),
                winner.getOverrideDomain().orElse(null),
                winner.getOverrideEngineProfile().orElse(null),
                winner.getOverrideCoreRole().orElse(null),
                winner.getOverrideDomainRole().orElse(null),
                appliedRuleIds
        );
    }

    private List<MatchedRule> collectMatchedRules(UnifiedGeneratePromptCommand command, IntentDefaults defaults) {
        List<MatchedRule> matched = new ArrayList<>();
        for (int i = 0; i < rules.size(); i++) {
            RoutingRule rule = rules.get(i);
            if (rule.matches(command, defaults)) {
                matched.add(new MatchedRule(rule, i));
            }
        }
        return matched;
    }

    private static Comparator<MatchedRule> byPriorityThenSpecificityThenOrder() {
        return Comparator
                .comparingInt((MatchedRule m) -> m.rule.getPriority()).reversed()
                .thenComparingInt((MatchedRule m) -> m.rule.getCondition().specificity()).reversed()
                .thenComparingInt(m -> m.index);
    }

    private record MatchedRule(RoutingRule rule, int index) {
    }
}
