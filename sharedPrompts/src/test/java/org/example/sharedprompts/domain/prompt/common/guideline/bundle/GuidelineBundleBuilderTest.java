package org.example.sharedprompts.domain.prompt.common.guideline.bundle;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.RuleLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GuidelineBundleBuilder 단위 테스트")
class GuidelineBundleBuilderTest {

    private final GuidelineBundleBuilder builder = new GuidelineBundleBuilder();

    @Test
    @DisplayName("도메인만으로 번들 생성 시 hard/soft 규칙 포함")
    void buildFromDomainContainsRules() {
        GuidelineBundle bundle = builder.build(TaskDomain.TECHNICAL);
        assertThat(bundle.hardRules()).isNotEmpty();
        assertThat(bundle.softRules()).isNotEmpty();
        assertThat(bundle.constraints()).isNotEmpty();
        assertThat(bundle.strategyHints()).isNotBlank();
    }

    @Test
    @DisplayName("토큰 예산 적용 시 규칙 수가 제한됨")
    void budgetLimitsRuleCount() {
        RuleBudgetPolicy strictBudget = new RuleBudgetPolicy(3, 500);
        GuidelineBundleBuilder limitedBuilder = new GuidelineBundleBuilder(strictBudget, null);
        GuidelineBundle bundle = limitedBuilder.build(TaskDomain.CREATIVE);
        int total = bundle.hardRules().size() + bundle.softRules().size();
        assertThat(total).isLessThanOrEqualTo(3);
    }

    @Test
    @DisplayName("GENERAL 도메인도 번들 생성 가능")
    void generalDomainBuilds() {
        GuidelineBundle bundle = builder.build(TaskDomain.GENERAL);
        assertThat(bundle.strategyHints()).contains("Adapt");
    }
}
