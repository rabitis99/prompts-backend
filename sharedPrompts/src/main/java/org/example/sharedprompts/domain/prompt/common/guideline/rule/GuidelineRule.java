package org.example.sharedprompts.domain.prompt.common.guideline.rule;

import org.example.sharedprompts.domain.prompt.common.guideline.i18n.I18nText;

/**
 * 가이드라인 규칙 record
 */
public record GuidelineRule(
        String id,
        I18nText title,
        I18nText description,
        RuleLevel level,
        RuleType type
) {
}
