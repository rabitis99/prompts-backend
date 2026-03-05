package org.example.sharedprompts.domain.prompt.domain.verification.guideline;

import org.example.sharedprompts.domain.prompt.common.guideline.rule.GuidelineRule;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.RuleType;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Default implementation of {@link GuidelineRuleChecker}.
 * FORBID rules: checks for common forbidden content (greetings, boilerplate).
 * REQUIRE rules: no automatic violation (heuristic-only; extend for stricter checks).
 */
public final class DefaultGuidelineRuleChecker implements GuidelineRuleChecker {

    private static final List<Pattern> GREETING_PATTERNS = List.of(
            Pattern.compile("(?i)(안녕|hello|hi there|dear (user|reader)|こんにちは)"),
            Pattern.compile("(?i)^\\s*(thanks?|thank you|감사합니다)\\s*[.!]?\\s*$")
    );

    @Override
    public boolean isViolated(String output, GuidelineRule rule) {
        if (output == null || output.isBlank()) {
            return rule.type() == RuleType.REQUIRE; // empty output violates REQUIRE
        }
        if (rule.type() == RuleType.FORBID) {
            return matchesForbiddenPattern(output, rule);
        }
        // REQUIRE/ALLOW: no generic violation by default
        return false;
    }

    private boolean matchesForbiddenPattern(String output, GuidelineRule rule) {
        String id = rule.id() != null ? rule.id().toUpperCase(Locale.ROOT) : "";
        if (id.contains("NO_GREETING") || id.contains("GREETING")) {
            return GREETING_PATTERNS.stream().anyMatch(p -> p.matcher(output.trim()).find());
        }
        return false;
    }
}
