package org.example.sharedprompts.domain.prompt.domain.verification.guideline;

import org.example.sharedprompts.domain.prompt.common.guideline.rule.GuidelineRule;

import java.util.ArrayList;
import java.util.List;

public final class GuidelineVerifier {

    private final GuidelineRuleChecker ruleChecker;

    public GuidelineVerifier(GuidelineRuleChecker ruleChecker) {
        this.ruleChecker = ruleChecker != null ? ruleChecker : new DefaultGuidelineRuleChecker();
    }

    /**
     * Verify output against the given rules. Rules are interpreted by level and type:
     * HARD + FORBID/REQUIRE → failure on violation; SOFT → warning only.
     */
    public GuidelineVerificationResult verify(String output, List<GuidelineRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return GuidelineVerificationResult.pass(List.of());
        }

        List<GuidelineViolation> failures = new ArrayList<>();
        List<GuidelineViolation> warnings = new ArrayList<>();

        for (GuidelineRule rule : rules) {
            if (rule == null) continue;
            boolean violated = ruleChecker.isViolated(output, rule);
            if (!violated) continue;

            GuidelineViolation v = new GuidelineViolation(
                    rule.id(),
                    rule.level(),
                    rule.type(),
                    rule.description() != null ? rule.description().en() : "Rule " + rule.id() + " violated"
            );
            if (v.isFailure()) {
                failures.add(v);
            } else {
                warnings.add(v);
            }
        }

        return failures.isEmpty()
                ? GuidelineVerificationResult.pass(warnings)
                : GuidelineVerificationResult.fail(failures, warnings);
    }
}
