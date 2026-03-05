package org.example.sharedprompts.domain.prompt.common.guideline.context;

import org.example.sharedprompts.domain.prompt.common.guideline.rule.GuidelineRule;

@FunctionalInterface
public interface GuidelineRuleApplicability {

    boolean applies(GuidelineRule rule, RuleContext context);

    static GuidelineRuleApplicability defaultApplicability() {
        return (rule, ctx) -> {
            if (rule == null || rule.id() == null) return true;
            String id = rule.id().toUpperCase();
            boolean isCreativeAnti = id.contains("CREATIVE.ANTI") || id.contains("NO_FORCED_");
            if (!isCreativeAnti) return true;
            return !ctx.requiresStructuredOutput() && !ctx.hasJsonSchema();
        };
    }
}
