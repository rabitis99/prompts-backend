package org.example.sharedprompts.domain.prompt.common.guideline.bundle;

import org.example.sharedprompts.domain.prompt.common.guideline.rule.GuidelineRule;

import java.util.List;

/**
 * Single assembled set of guidelines for prompt rendering.
 * Built from TaskDomain + context; used by PromptSpecFactory and PromptGuidelineBuilder
 * to avoid duplicating rule text across checklist, essential constraints, and strategy sections.
 */
public record GuidelineBundle(
        List<GuidelineRule> hardRules,
        List<GuidelineRule> softRules,
        List<GuidelineRule> constraints,
        String strategyHints
) {
    public GuidelineBundle {
        hardRules = hardRules == null ? List.of() : List.copyOf(hardRules);
        softRules = softRules == null ? List.of() : List.copyOf(softRules);
        constraints = constraints == null ? List.of() : List.copyOf(constraints);
        strategyHints = strategyHints == null ? "" : strategyHints;
    }
}
