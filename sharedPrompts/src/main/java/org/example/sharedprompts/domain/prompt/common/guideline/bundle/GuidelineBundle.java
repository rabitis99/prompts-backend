package org.example.sharedprompts.domain.prompt.common.guideline.bundle;

import org.example.sharedprompts.domain.prompt.common.guideline.rule.GuidelineRule;

import java.util.Collections;
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
    public List<GuidelineRule> hardRules() {
        return hardRules == null ? List.of() : Collections.unmodifiableList(hardRules);
    }

    public List<GuidelineRule> softRules() {
        return softRules == null ? List.of() : Collections.unmodifiableList(softRules);
    }

    public List<GuidelineRule> constraints() {
        return constraints == null ? List.of() : Collections.unmodifiableList(constraints);
    }

    public String strategyHints() {
        return strategyHints != null ? strategyHints : "";
    }
}
