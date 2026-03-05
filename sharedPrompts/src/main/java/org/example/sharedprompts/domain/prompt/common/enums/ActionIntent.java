package org.example.sharedprompts.domain.prompt.common.enums;

import org.example.sharedprompts.domain.prompt.common.enums.role.CoreRoleType;

import java.util.Optional;

/**
 * 상위 레벨 작업 의도.
 *
 * <p>각 Intent는 기본 Objective, 선호 OutputNeeds, 기본 ResponseShape,
 * 선택적인 TaskDomain affinity, 기본 CoreRoleType 메타데이터를 가진다.</p>
 */
public enum ActionIntent {

    GENERATE(
            PromptObjective.CREATIVE,
            OutputNeeds.FREE_FORM,
            ResponseShape.NARRATIVE,
            null,
            CoreRoleType.CREATIVE_DIRECTOR
    ),

    REWRITE(
            PromptObjective.CREATIVE,
            OutputNeeds.FREE_FORM,
            ResponseShape.STRUCTURED,
            null,
            CoreRoleType.EDITOR
    ),

    SUMMARIZE(
            PromptObjective.FACTUAL,
            OutputNeeds.BULLET_LIST_REQUIRED,
            ResponseShape.CONCISE,
            TaskDomain.ANALYTICAL,
            CoreRoleType.ANALYST
    ),

    EXPLAIN(
            PromptObjective.REASONING,
            OutputNeeds.FREE_FORM,
            ResponseShape.STEP_BY_STEP,
            TaskDomain.EDUCATIONAL,
            CoreRoleType.EDUCATOR
    ),

    PLAN(
            PromptObjective.PLANNING,
            OutputNeeds.STRUCTURED_TEXT,
            ResponseShape.STEP_BY_STEP,
            TaskDomain.PRACTICAL,
            CoreRoleType.PROMPT_ENGINEER
    ),

    ANALYZE(
            PromptObjective.REASONING,
            OutputNeeds.STRUCTURED_TEXT,
            ResponseShape.STRUCTURED,
            TaskDomain.ANALYTICAL,
            CoreRoleType.ANALYST
    ),

    EVALUATE(
            PromptObjective.REASONING,
            OutputNeeds.STRUCTURED_TEXT,
            ResponseShape.STRUCTURED,
            TaskDomain.ANALYTICAL,
            CoreRoleType.ANALYST
    ),

    EXTRACT(
            PromptObjective.EXTRACTION,
            OutputNeeds.JSON_REQUIRED,
            ResponseShape.STRUCTURED,
            TaskDomain.ANALYTICAL,
            CoreRoleType.ANALYST
    ),

    CLASSIFY(
            PromptObjective.EXTRACTION,
            OutputNeeds.JSON_REQUIRED,
            ResponseShape.STRUCTURED,
            TaskDomain.ANALYTICAL,
            CoreRoleType.ANALYST
    ),

    DECIDE(
            PromptObjective.REASONING,
            OutputNeeds.STRUCTURED_TEXT,
            ResponseShape.STRUCTURED,
            TaskDomain.PRACTICAL,
            CoreRoleType.PROMPT_ENGINEER
    ),

    DEBUG(
            PromptObjective.REASONING,
            OutputNeeds.STRUCTURED_TEXT,
            ResponseShape.STEP_BY_STEP,
            TaskDomain.TECHNICAL,
            CoreRoleType.TECHNICAL_EXPERT
    ),

    DESIGN(
            PromptObjective.CREATIVE,
            OutputNeeds.STRUCTURED_TEXT,
            ResponseShape.STRUCTURED,
            TaskDomain.CREATIVE,
            CoreRoleType.CREATIVE_DIRECTOR
    ),

    CODE(
            PromptObjective.CODE,
            OutputNeeds.CODE_BLOCK_REQUIRED,
            ResponseShape.STRUCTURED,
            TaskDomain.TECHNICAL,
            CoreRoleType.TECHNICAL_EXPERT
    );

    private final PromptObjective defaultObjective;
    private final OutputNeeds preferredOutputNeeds;
    private final ResponseShape defaultResponseShape;
    private final TaskDomain domainAffinity; // nullable
    private final CoreRoleType defaultCoreRole;

    ActionIntent(PromptObjective defaultObjective,
                 OutputNeeds preferredOutputNeeds,
                 ResponseShape defaultResponseShape,
                 TaskDomain domainAffinity,
                 CoreRoleType defaultCoreRole) {
        this.defaultObjective = defaultObjective;
        this.preferredOutputNeeds = preferredOutputNeeds;
        this.defaultResponseShape = defaultResponseShape;
        this.domainAffinity = domainAffinity;
        this.defaultCoreRole = defaultCoreRole;
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

    /**
     * 이 Intent에 대한 기본 코어 역할. 라우팅 시 오버라이드 없으면 이 값이 사용된다.
     */
    public CoreRoleType getDefaultCoreRole() {
        return defaultCoreRole;
    }
}

