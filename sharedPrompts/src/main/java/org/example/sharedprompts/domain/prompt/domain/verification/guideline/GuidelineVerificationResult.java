package org.example.sharedprompts.domain.prompt.domain.verification.guideline;

import java.util.List;

public record GuidelineVerificationResult(
        boolean passed,
        List<GuidelineViolation> failures,
        List<GuidelineViolation> warnings
) {
    public GuidelineVerificationResult {
        failures = failures == null ? List.of() : List.copyOf(failures);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        passed = failures.isEmpty();
    }

    public static GuidelineVerificationResult pass(List<GuidelineViolation> warnings) {
        return new GuidelineVerificationResult(true, List.of(), warnings);
    }

    public static GuidelineVerificationResult fail(List<GuidelineViolation> failures, List<GuidelineViolation> warnings) {
        return new GuidelineVerificationResult(
                false,
                failures,
                warnings
        );
    }

    public List<GuidelineViolation> failures() {
        return failures;
    }

    public List<GuidelineViolation> warnings() {
        return warnings;
    }
}
