package org.example.sharedprompts.domain.prompt.common.enums;

import java.util.Optional;

/**
 * 상위 레벨 작업 의도.
 *
 * <p>각 Intent는 기본 Objective, 선호 OutputNeeds, 기본 ResponseShape,
 * 선택적인 TaskDomain affinity 메타데이터를 가진다.</p>
 */
public enum ActionIntent {

    GENERATE(
            PromptObjective.CREATIVE,
            OutputNeeds.FREE_FORM,
            ResponseShape.NARRATIVE,
            null
    ),

    REWRITE(
            PromptObjective.CREATIVE,
            OutputNeeds.FREE_FORM,
            ResponseShape.STRUCTURED,
            null
    ),

    SUMMARIZE(
            PromptObjective.FACTUAL,
            OutputNeeds.BULLET_LIST_REQUIRED,
            ResponseShape.CONCISE,
            TaskDomain.ANALYTICAL
    ),

    EXPLAIN(
            PromptObjective.REASONING,
            OutputNeeds.FREE_FORM,
            ResponseShape.STEP_BY_STEP,
            TaskDomain.EDUCATIONAL
    ),

    PLAN(
            PromptObjective.PLANNING,
            OutputNeeds.STRUCTURED_TEXT,
            ResponseShape.STEP_BY_STEP,
            TaskDomain.PRACTICAL
    ),

    ANALYZE(
            PromptObjective.REASONING,
            OutputNeeds.STRUCTURED_TEXT,
            ResponseShape.STRUCTURED,
            TaskDomain.ANALYTICAL
    ),

    EVALUATE(
            PromptObjective.REASONING,
            OutputNeeds.STRUCTURED_TEXT,
            ResponseShape.STRUCTURED,
            TaskDomain.ANALYTICAL
    ),

    EXTRACT(
            PromptObjective.EXTRACTION,
            OutputNeeds.JSON_REQUIRED,
            ResponseShape.STRUCTURED,
            TaskDomain.ANALYTICAL
    ),

    CLASSIFY(
            PromptObjective.EXTRACTION,
            OutputNeeds.JSON_REQUIRED,
            ResponseShape.STRUCTURED,
            TaskDomain.ANALYTICAL
    ),

    DECIDE(
            PromptObjective.REASONING,
            OutputNeeds.STRUCTURED_TEXT,
            ResponseShape.STRUCTURED,
            TaskDomain.PRACTICAL
    ),

    DEBUG(
            PromptObjective.REASONING,
            OutputNeeds.STRUCTURED_TEXT,
            ResponseShape.STEP_BY_STEP,
            TaskDomain.TECHNICAL
    ),

    DESIGN(
            PromptObjective.CREATIVE,
            OutputNeeds.STRUCTURED_TEXT,
            ResponseShape.STRUCTURED,
            TaskDomain.CREATIVE
    ),

    CODE(
            PromptObjective.CODE,
            OutputNeeds.CODE_BLOCK_REQUIRED,
            ResponseShape.STRUCTURED,
            TaskDomain.TECHNICAL
    );

    private final PromptObjective defaultObjective;
    private final OutputNeeds preferredOutputNeeds;
    private final ResponseShape defaultResponseShape;
    private final TaskDomain domainAffinity; // nullable

    ActionIntent(PromptObjective defaultObjective,
                 OutputNeeds preferredOutputNeeds,
                 ResponseShape defaultResponseShape,
                 TaskDomain domainAffinity) {
        this.defaultObjective = defaultObjective;
        this.preferredOutputNeeds = preferredOutputNeeds;
        this.defaultResponseShape = defaultResponseShape;
        this.domainAffinity = domainAffinity;
    }

    public PromptObjective getDefaultObjective() {
        return defaultObjective;
    }

    public OutputNeeds getPreferredOutputNeeds() {
        return preferredOutputNeeds;
    }

    public ResponseShape getDefaultResponseShape() {
        return defaultResponseShape;
    }

    /**
     * 선택적인 도메인 선호도.
     */
    public Optional<TaskDomain> getDomainAffinity() {
        return Optional.ofNullable(domainAffinity);
    }
}

