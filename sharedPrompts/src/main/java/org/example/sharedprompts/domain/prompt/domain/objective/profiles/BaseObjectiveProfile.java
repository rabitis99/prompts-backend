package org.example.sharedprompts.domain.prompt.domain.objective.profiles;

import org.example.sharedprompts.domain.prompt.domain.model.Constraints;
import org.example.sharedprompts.domain.prompt.domain.model.OutputContract;
import org.example.sharedprompts.domain.prompt.domain.objective.ObjectiveProfile;
import org.example.sharedprompts.domain.prompt.enums.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.enums.LanguageType;

/**
 * 모든 ObjectiveProfile 구현체가 공유하는 스케일링 유틸리티.
 * ExperienceLevel 배율 적용, freeText OutputContract 생성 등 공통 로직을 제공한다.
 */
abstract class BaseObjectiveProfile implements ObjectiveProfile {

    /** ExperienceLevel에 따른 maxLength 배율 */
    protected int scaledMaxLength(int base, ExperienceLevel level) {
        double factor = switch (level) {
            case BEGINNER -> 1.2;
            case INTERMEDIATE -> 1.0;
            case ADVANCED -> 0.9;
            case EXPERT -> 0.75;
        };
        return (int) Math.round(base * factor);
    }

    /** 대부분의 Objective에서 공유하는 Constraints 빌더 */
    protected Constraints buildConstraints(int baseMaxLength,
                                           boolean requireStepByStep,
                                           boolean requireCitations,
                                           ExperienceLevel level) {
        int maxLength = scaledMaxLength(baseMaxLength, level);
        boolean stepByStep = requireStepByStep || level == ExperienceLevel.BEGINNER;
        return Constraints.builder()
                .maxLength(maxLength)
                .requireStepByStep(stepByStep)
                .requireCitations(requireCitations)
                .build();
    }

    /** EXTRACTION 외 Objective의 기본 OutputContract */
    protected OutputContract freeTextContract(int maxTokens) {
        return OutputContract.freeText(maxTokens);
    }

    /** jsonSchema 미제공 시 사용할 기본 EXTRACTION 스키마 */
    protected static final String DEFAULT_EXTRACTION_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "result": { "type": "string" }
              },
              "required": ["result"]
            }
            """;

    /** locale에 따라 한국어/기타(영어) 문구를 선택한다 */
    protected String i18n(LanguageType locale, String ko, String en) {
        return locale == LanguageType.KOREAN ? ko : en;
    }
}
