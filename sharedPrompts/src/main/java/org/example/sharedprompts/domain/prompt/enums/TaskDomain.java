package org.example.sharedprompts.domain.prompt.enums;

import lombok.Getter;
import org.example.sharedprompts.domain.prompt.enums.guideline.*;

import java.util.List;

/**
 * 작업 도메인 — PromptCategory/ActionType에 따라 적합한 가이드라인 정책을 결정
 * <p>GuidelinePolicy를 delegate 패턴으로 구현하여 enum 비대화를 방지한다.</p>
 */
@Getter
public enum TaskDomain implements GuidelinePolicy {
    /** 정확성/실용성 중심 기술 작업 */
    TECHNICAL(TechnicalGuidelines.INSTANCE),
    /** 독창성/감성 중심 창작 작업 */
    CREATIVE(CreativeGuidelines.INSTANCE),
    /** 증거 기반 체계적 분석 */
    ANALYTICAL(AnalyticalGuidelines.INSTANCE),
    /** 즉시 활용 가능한 실무 */
    PRACTICAL(PracticalGuidelines.INSTANCE),
    /** 이해 촉진/단계적 학습 */
    EDUCATIONAL(EducationalGuidelines.INSTANCE),
    /** 보수적 안전 모드 */
    GENERAL(GeneralGuidelines.INSTANCE);

    private final GuidelinePolicy delegate;

    TaskDomain(GuidelinePolicy delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<GuidelineRule> principles() {
        return delegate.principles();
    }

    @Override
    public List<GuidelineRule> structuringRules() {
        return delegate.structuringRules();
    }

    @Override
    public List<GuidelineRule> qualityStandards() {
        return delegate.qualityStandards();
    }

    @Override
    public List<GuidelineRule> outputConstraints() {
        return delegate.outputConstraints();
    }

    /**
     * 이 TaskDomain에 추천되는 StyleType 목록을 반환한다.
     * <p>도메인 특성에 맞는 스타일을 제안한다.</p>
     *
     * @return 추천되는 StyleType 목록 (비어있지 않음)
     */
    public List<StyleType> getRecommendedStyles() {
        return switch (this) {
            case TECHNICAL -> List.of(
                    StyleType.TECHNICAL,
                    StyleType.FORMATTED,
                    StyleType.INSTRUCTIVE,
                    StyleType.CONCISE
            );
            case CREATIVE -> List.of(
                    StyleType.NARRATIVE,
                    StyleType.STORYTELLING,
                    StyleType.DESCRIPTIVE,
                    StyleType.CREATIVE,
                    StyleType.DIALOGUE
            );
            case ANALYTICAL -> List.of(
                    StyleType.ANALYTICAL,
                    StyleType.COMPARATIVE,
                    StyleType.QUESTION_ANSWER
            );
            case PRACTICAL -> List.of(
                    StyleType.BULLET,
                    StyleType.CONCISE,
                    StyleType.INSTRUCTIVE,
                    StyleType.FORMATTED
            );
            case EDUCATIONAL -> List.of(
                    StyleType.INSTRUCTIVE,
                    StyleType.DETAILED,
                    StyleType.QUESTION_ANSWER
            );
            case GENERAL -> List.of(StyleType.values()); // 모든 스타일 허용
        };
    }

    /**
     * 이 TaskDomain에 추천되는 ToneType 목록을 반환한다.
     * <p>도메인 특성에 맞는 톤을 제안한다.</p>
     *
     * @return 추천되는 ToneType 목록 (비어있지 않음)
     */
    public List<ToneType> getRecommendedTones() {
        return switch (this) {
            case TECHNICAL -> List.of(
                    ToneType.PROFESSIONAL,
                    ToneType.FORMAL,
                    ToneType.NEUTRAL
            );
            case CREATIVE -> List.of(
                    ToneType.INSPIRATIONAL,
                    ToneType.ENTHUSIASTIC,
                    ToneType.FRIENDLY,
                    ToneType.HUMOROUS,
                    ToneType.CASUAL
            );
            case ANALYTICAL -> List.of(
                    ToneType.NEUTRAL,
                    ToneType.PROFESSIONAL,
                    ToneType.FORMAL
            );
            case PRACTICAL -> List.of(
                    ToneType.PROFESSIONAL,
                    ToneType.FRIENDLY,
                    ToneType.MOTIVATIONAL,
                    ToneType.POSITIVE
            );
            case EDUCATIONAL -> List.of(
                    ToneType.FRIENDLY,
                    ToneType.MOTIVATIONAL,
                    ToneType.ENTHUSIASTIC,
                    ToneType.EMPATHETIC
            );
            case GENERAL -> List.of(ToneType.values()); // 모든 톤 허용
        };
    }
}
