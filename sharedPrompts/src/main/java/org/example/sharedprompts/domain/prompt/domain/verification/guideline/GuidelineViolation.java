package org.example.sharedprompts.domain.prompt.domain.verification.guideline;

import org.example.sharedprompts.domain.prompt.common.guideline.rule.RuleLevel;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.RuleType;

public record GuidelineViolation(
        String ruleId,
        RuleLevel level,
        RuleType type,
        String message
) {
    public boolean isFailure() {
        return type == RuleType.FORBID || level == RuleLevel.HARD;
    }

    public boolean isWarningOnly() {
        return level == RuleLevel.SOFT && type != RuleType.FORBID;
    }
}
