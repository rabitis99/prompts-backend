package org.example.sharedprompts.domain.prompt.enums;

import org.example.sharedprompts.domain.prompt.enums.guideline.*;

import java.util.List;
import java.util.stream.Stream;

/**
 * 작업 도메인 — PromptCategory/ActionType에 따라 적합한 가이드라인 정책을 결정
 * <p>GuidelinePolicy를 delegate 패턴으로 구현하여 enum 비대화를 방지한다.</p>
 */
public enum TaskDomain implements GuidelinePolicy {
    /** 정확성/실용성 중심 기술 작업 */
    TECHNICAL("기술형", TechnicalGuidelines.INSTANCE),
    /** 독창성/감성 중심 창작 작업 */
    CREATIVE("창의형", CreativeGuidelines.INSTANCE),
    /** 증거 기반 체계적 분석 */
    ANALYTICAL("분석형", AnalyticalGuidelines.INSTANCE),
    /** 즉시 활용 가능한 실무 */
    PRACTICAL("실무형", PracticalGuidelines.INSTANCE),
    /** 이해 촉진/단계적 학습 */
    EDUCATIONAL("교육형", EducationalGuidelines.INSTANCE),
    /** 보수적 안전 모드 */
    GENERAL("일반형", GeneralGuidelines.INSTANCE);

    /** UI 표시용 */
    private final String displayName;
    private final GuidelinePolicy delegate;

    TaskDomain(String displayName, GuidelinePolicy delegate) {
        this.displayName = displayName;
        this.delegate = delegate;
    }

    /**
     * UI 표시용 이름을 반환한다.
     */
    public String getDisplayName() {
        return displayName;
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

    /**
     * 지정된 레벨의 규칙을 모든 카테고리에서 추출하여 반환한다.
     * <p>principles, structuringRules, qualityStandards, outputConstraints를 통합하여 필터링한다.</p>
     *
     * @param level 필터링할 규칙 레벨
     * @return 해당 레벨의 규칙 목록
     */
    public List<GuidelineRule> getRulesByLevel(RuleLevel level) {
        return Stream.of(
                principles().stream(),
                structuringRules().stream(),
                qualityStandards().stream(),
                outputConstraints().stream()
        ).flatMap(s -> s)
         .filter(rule -> rule.level() == level)
         .toList();
    }
}
