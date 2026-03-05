package org.example.sharedprompts.domain.prompt.common.guideline.bundle;

import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.GuidelineRule;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.RuleLevel;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.RuleType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
     * Priority: HARD+FORBID > HARD+REQUIRE > SOFT+FORBID > SOFT+REQUIRE > SOFT+ALLOW (others are lowest).
     * <p>
     * 예산 초과 시 해당 규칙은 건너뛰고(continue) 다음 규칙을 시도하는
     * greedy 전략을 사용하므로, 더 짧은 저우선순위 규칙이 일부 포함될 수 있습니다.
     */
    public List<GuidelineRule> selectRules(List<GuidelineRule> rules) {
        if (rules == null || rules.isEmpty()) return List.of();

        Comparator<GuidelineRule> byPriority =
                Comparator.comparingInt(RuleBudgetPolicy::priorityRank);

        List<GuidelineRule> sorted = rules.stream()
                .filter(Objects::nonNull)
                .sorted(byPriority)
                .toList();
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
        int idLen = rule.id() != null ? rule.id().length() : 0;
        if (rule.description() == null) return idLen;

        int koLen = rule.description().ko() != null ? rule.description().ko().length() : 0;
        int enLen = rule.description().en() != null ? rule.description().en().length() : 0;
        int jaLen = rule.description().ja() != null ? rule.description().ja().length() : 0;
        int descLen = Math.max(koLen, Math.max(enLen, jaLen));
        return idLen + descLen;
    }

    private static int priorityRank(GuidelineRule rule) {
        if (rule == null) return 5;
        if (rule.level() == RuleLevel.HARD && rule.type() == RuleType.FORBID) return 0;
        if (rule.level() == RuleLevel.HARD && rule.type() == RuleType.REQUIRE) return 1;
        if (rule.level() == RuleLevel.SOFT && rule.type() == RuleType.FORBID) return 2;
        if (rule.level() == RuleLevel.SOFT && rule.type() == RuleType.REQUIRE) return 3;
        if (rule.level() == RuleLevel.SOFT && rule.type() == RuleType.ALLOW) return 4;
        return 5;
    }
}
