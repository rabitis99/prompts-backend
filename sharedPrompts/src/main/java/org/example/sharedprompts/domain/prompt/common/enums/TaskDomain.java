package org.example.sharedprompts.domain.prompt.common.enums;

import org.example.sharedprompts.domain.prompt.common.guideline.policy.GuidelinePolicy;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.GuidelineRule;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.RuleLevel;
import org.example.sharedprompts.domain.prompt.common.guideline.content.TechnicalGuidelines;
import org.example.sharedprompts.domain.prompt.common.guideline.content.CreativeGuidelines;
import org.example.sharedprompts.domain.prompt.common.guideline.content.AnalyticalGuidelines;
import org.example.sharedprompts.domain.prompt.common.guideline.content.PracticalGuidelines;
import org.example.sharedprompts.domain.prompt.common.guideline.content.EducationalGuidelines;
import org.example.sharedprompts.domain.prompt.common.guideline.content.GeneralGuidelines;

import java.util.List;
import java.util.stream.Stream;

/**
 * 작업 도메인 — PromptCategory/ActionType에 따라 적합한 가이드라인 정책을 결정
 * <p>GuidelinePolicy를 delegate 패턴으로 구현하여 enum 비대화를 방지한다.</p>
 */
public enum TaskDomain implements GuidelinePolicy, StableKeyedEnum {
    /** 정확성/실용성 중심 기술 작업 */
    TECHNICAL("TASK_DOMAIN.TECHNICAL", "기술형", TechnicalGuidelines.INSTANCE),
    /** 독창성/감성 중심 창작 작업 */
    CREATIVE("TASK_DOMAIN.CREATIVE", "창의형", CreativeGuidelines.INSTANCE),
    /** 증거 기반 체계적 분석 */
    ANALYTICAL("TASK_DOMAIN.ANALYTICAL", "분석형", AnalyticalGuidelines.INSTANCE),
    /** 즉시 활용 가능한 실무 */
    PRACTICAL("TASK_DOMAIN.PRACTICAL", "실무형", PracticalGuidelines.INSTANCE),
    /** 이해 촉진/단계적 학습 */
    EDUCATIONAL("TASK_DOMAIN.EDUCATIONAL", "교육형", EducationalGuidelines.INSTANCE),
    /** 보수적 안전 모드 */
    GENERAL("TASK_DOMAIN.GENERAL", "일반형", GeneralGuidelines.INSTANCE);

    /** Stable serialization-safe identifier */
    private final String key;

    /** UI 표시용 */
    private final String displayName;
    private final GuidelinePolicy delegate;

    TaskDomain(String key, String displayName, GuidelinePolicy delegate) {
        this.key = key;
        this.displayName = displayName;
        this.delegate = delegate;
    }

    @Override
    public String key() {
        return key;
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
