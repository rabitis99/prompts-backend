package org.example.sharedprompts.domain.prompt.domain.verification.guideline;

import org.example.sharedprompts.domain.prompt.common.guideline.rule.GuidelineRule;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.RuleType;

/**
 * Strategy for detecting whether a given output violates a single guideline rule.
 * Default implementation uses simple heuristics; can be replaced for domain-specific checks.
 */
@FunctionalInterface
public interface GuidelineRuleChecker {

    /**
     * @return true if the output violates this rule (e.g. FORBID: forbidden phrase present; REQUIRE: required element missing).
     */
    boolean isViolated(String output, GuidelineRule rule);

    /**
     * Default checker: FORBID rules check for common forbidden patterns (greetings, fluff);
     * REQUIRE rules are not deeply validated (no violation by default to avoid false positives).
     */
    static GuidelineRuleChecker defaultChecker() {
        return new DefaultGuidelineRuleChecker();
    }
}
