package org.example.sharedprompts.domain.prompt.common.guideline.bundle;

import org.example.sharedprompts.domain.prompt.common.guideline.i18n.I18nText;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.GuidelineRule;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.RuleLevel;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.RuleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RuleBudgetPolicy 단위 테스트")
class RuleBudgetPolicyTest {

    private static GuidelineRule rule(String id, RuleLevel level, RuleType type) {
        return new GuidelineRule(
                id,
                I18nText.of("제목", "Title", "タイトル"),
                I18nText.of("설명", "Description " + id, "説明"),
                level,
                type
        );
    }

    @Test
    @DisplayName("maxRules 초과 시 우선순위대로 잘림")
    void selectRulesRespectsMaxRules() {
        RuleBudgetPolicy policy = new RuleBudgetPolicy(2, 10_000);
        List<GuidelineRule> rules = List.of(
                rule("SOFT.ALLOW", RuleLevel.SOFT, RuleType.ALLOW),
                rule("HARD.FORBID", RuleLevel.HARD, RuleType.FORBID),
                rule("HARD.REQUIRE", RuleLevel.HARD, RuleType.REQUIRE)
        );
        List<GuidelineRule> selected = policy.selectRules(rules);
        assertThat(selected).hasSize(2);
        assertThat(selected.get(0).id()).isEqualTo("HARD.FORBID");
        assertThat(selected.get(1).id()).isEqualTo("HARD.REQUIRE");
    }

    @Test
    @DisplayName("우선순위: HARD+FORBID > HARD+REQUIRE > SOFT+REQUIRE > SOFT+ALLOW")
    void selectionPriorityOrder() {
        RuleBudgetPolicy policy = new RuleBudgetPolicy(10, 10_000);
        List<GuidelineRule> rules = List.of(
                rule("S.A", RuleLevel.SOFT, RuleType.ALLOW),
                rule("H.F", RuleLevel.HARD, RuleType.FORBID),
                rule("S.R", RuleLevel.SOFT, RuleType.REQUIRE),
                rule("H.R", RuleLevel.HARD, RuleType.REQUIRE)
        );
        List<GuidelineRule> selected = policy.selectRules(rules);
        assertThat(selected.stream().map(GuidelineRule::id).toList())
                .containsExactly("H.F", "H.R", "S.R", "S.A");
    }

    @Test
    @DisplayName("빈 목록이면 빈 결과")
    void emptyInputReturnsEmpty() {
        RuleBudgetPolicy policy = new RuleBudgetPolicy(5, 1000);
        assertThat(policy.selectRules(List.of())).isEmpty();
        assertThat(policy.selectRules(null)).isEmpty();
    }
}
