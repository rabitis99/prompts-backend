package org.example.sharedprompts.domain.prompt.common.guideline.bundle;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.guideline.context.GuidelineRuleApplicability;
import org.example.sharedprompts.domain.prompt.common.guideline.context.RuleContext;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.GuidelineRule;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.RuleLevel;

import java.util.ArrayList;
import java.util.List;

public final class GuidelineBundleBuilder {

    private final RuleBudgetPolicy budgetPolicy;
    private final GuidelineRuleApplicability applicability;

    public GuidelineBundleBuilder(RuleBudgetPolicy budgetPolicy, GuidelineRuleApplicability applicability) {
        this.budgetPolicy = budgetPolicy != null ? budgetPolicy : new RuleBudgetPolicy();
        this.applicability = applicability;
    }

    public GuidelineBundleBuilder() {
        this(new RuleBudgetPolicy(), null);
    }

    public GuidelineBundle build(TaskDomain domain) {
        return build(domain, null);
    }

    public GuidelineBundle build(TaskDomain domain, RuleContext ruleContext) {
        TaskDomain resolvedDomain = (domain != null) ? domain : TaskDomain.GENERAL;
        List<GuidelineRule> allHard = resolvedDomain.getRulesByLevel(RuleLevel.HARD);
        List<GuidelineRule> allSoft = resolvedDomain.getRulesByLevel(RuleLevel.SOFT);
        List<GuidelineRule> all = new ArrayList<>();
        all.addAll(filterApplicable(allHard, ruleContext));
        all.addAll(filterApplicable(allSoft, ruleContext));

        List<GuidelineRule> selected = budgetPolicy.selectRules(all);
        List<GuidelineRule> hardRules = selected.stream().filter(r -> r.level() == RuleLevel.HARD).toList();
        List<GuidelineRule> softRules = selected.stream().filter(r -> r.level() == RuleLevel.SOFT).toList();
        List<GuidelineRule> constraints = new ArrayList<>(hardRules);

        String strategyHints = buildStrategyHints(resolvedDomain);
        return new GuidelineBundle(hardRules, softRules, constraints, strategyHints);
    }

    private List<GuidelineRule> filterApplicable(List<GuidelineRule> rules, RuleContext ctx) {
        if (applicability == null || ctx == null) return rules;
        return rules.stream()
                .filter(r -> applicability.applies(r, ctx))
                .toList();
    }

    private static String buildStrategyHints(TaskDomain domain) {
        if (domain == null) return "";
        return switch (domain) {
            case TECHNICAL -> "Name specific technologies; request concrete implementation details; include performance/security/scalability.";
            case CREATIVE -> "Encourage original perspectives; emotional resonance; avoid rigid structure and list-based formatting.";
            case ANALYTICAL -> "Request evidence-based reasoning; multi-perspective analysis; clear cause-effect and summary.";
            case PRACTICAL -> "Request actionable steps and concrete outputs; include realistic constraints.";
            case EDUCATIONAL -> "Build from foundational to advanced; include examples and self-check questions.";
            case GENERAL -> "Adapt to the request; balanced, accessible guidance.";
        };
    }
}
