package org.example.sharedprompts.domain.prompt.common.guideline.bundle;

import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.GuidelineRule;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.RuleLevel;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.RuleType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
@Getter
public final class RuleBudgetPolicy {

    private static final int DEFAULT_MAX_RULES = 20;
    private static final int DEFAULT_MAX_CHARACTERS = 2_000;

    private final int maxRules;
    private final int maxCharacters;

    public RuleBudgetPolicy(int maxRules, int maxCharacters) {
        this.maxRules = maxRules > 0 ? maxRules : DEFAULT_MAX_RULES;
        this.maxCharacters = maxCharacters > 0 ? maxCharacters : DEFAULT_MAX_CHARACTERS;
    }

    public RuleBudgetPolicy() {
        this(DEFAULT_MAX_RULES, DEFAULT_MAX_CHARACTERS);
    }

    /**
     * Select rules up to maxRules and maxCharacters, ordered by importance.
     * Priority: HARD+FORBID > HARD+REQUIRE > SOFT+REQUIRE > SOFT+ALLOW.
     */
    public List<GuidelineRule> selectRules(List<GuidelineRule> rules) {
        if (rules == null || rules.isEmpty()) return List.of();

        Comparator<GuidelineRule> byPriority =
                Comparator.comparingInt(RuleBudgetPolicy::priorityRank);

        List<GuidelineRule> sorted = rules.stream().sorted(byPriority).toList();
        List<GuidelineRule> selected = new ArrayList<>();
        int totalChars = 0;

        for (GuidelineRule rule : sorted) {
            if (selected.size() >= maxRules) break;
            int ruleLen = estimateLength(rule);
            if (totalChars + ruleLen > maxCharacters) continue;
            selected.add(rule);
            totalChars += ruleLen;
        }
        return selected;
    }

    private static int estimateLength(GuidelineRule rule) {
        if (rule == null) return 0;
        String d = rule.description() != null ? rule.description().en() : "";
        return (rule.id() != null ? rule.id().length() : 0) + (d != null ? d.length() : 0);
    }

    private static int priorityRank(GuidelineRule rule) {
        if (rule.level() == RuleLevel.HARD && rule.type() == RuleType.FORBID) return 0;
        if (rule.level() == RuleLevel.HARD && rule.type() == RuleType.REQUIRE) return 1;
        if (rule.level() == RuleLevel.SOFT && rule.type() == RuleType.REQUIRE) return 2;
        if (rule.level() == RuleLevel.SOFT && rule.type() == RuleType.ALLOW) return 3;
        return 4;
    }
}
