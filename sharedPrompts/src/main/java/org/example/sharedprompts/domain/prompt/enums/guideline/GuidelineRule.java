package org.example.sharedprompts.domain.prompt.enums.guideline;

/**
 * 가이드라인 규칙 record
 *
 * @param id          고유 식별자 (e.g. "TECH.PRINCIPLE.ACCURACY")
 * @param title       규칙 제목 (다국어)
 * @param description 규칙 설명 (다국어)
 * @param level       강도 (HARD / SOFT)
 * @param type        방향 (REQUIRE / FORBID / ALLOW)
 */
public record GuidelineRule(
        String id,
        I18nText title,
        I18nText description,
        RuleLevel level,
        RuleType type
) {

    public static GuidelineRule of(String id, I18nText title, I18nText description,
                                   RuleLevel level, RuleType type) {
        return new GuidelineRule(id, title, description, level, type);
    }
}
