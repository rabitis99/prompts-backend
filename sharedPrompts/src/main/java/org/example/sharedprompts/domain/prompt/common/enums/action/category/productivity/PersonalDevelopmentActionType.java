package org.example.sharedprompts.domain.prompt.common.enums.action.category.productivity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Optional;

/**
 * 자기계발 관련 액션 타입 enum
 *
 * <p>생성자 파라미터 순서 (모든 파라미터는 String 타입):
 * <ol>
 *   <li>stableKey - 안정 키 (직렬화/호환성용)</li>
 *   <li>displayNameKo - 표시 이름 (한국어)</li>
 *   <li>displayNameEn - 표시 이름 (영어)</li>
 *   <li>displayNameJa - 표시 이름 (일본어)</li>
 * </ol>
 */
@Getter
@AllArgsConstructor
public enum PersonalDevelopmentActionType implements ActionTypeInterface, StableKeyedEnum {
    GOAL_SETTING("ACTION.PERSONAL_DEVELOPMENT.GOAL_SETTING", "목표 설정", "Goal Setting", "目標設定", OutputBehaviorType.STRATEGIC_PLAN),
    HABIT_FORMATION("ACTION.PERSONAL_DEVELOPMENT.HABIT_FORMATION", "습관 형성", "Habit Formation", "習慣形成", OutputBehaviorType.STRATEGIC_PLAN),
    SELF_IMPROVEMENT("ACTION.PERSONAL_DEVELOPMENT.SELF_IMPROVEMENT", "자기계발", "Self Improvement", "自己啓発", OutputBehaviorType.STRATEGIC_PLAN),
    STRESS_MANAGEMENT("ACTION.PERSONAL_DEVELOPMENT.STRESS_MANAGEMENT", "스트레스 관리", "Stress Management", "ストレス管理", OutputBehaviorType.STRATEGIC_PLAN),
    MOTIVATION("ACTION.PERSONAL_DEVELOPMENT.MOTIVATION", "동기 부여", "Motivation", "モチベーション", OutputBehaviorType.STRATEGIC_PLAN),
    DECISION_MAKING("ACTION.PERSONAL_DEVELOPMENT.DECISION_MAKING", "의사결정", "Decision Making", "意思決定", OutputBehaviorType.STRATEGIC_PLAN);

    private final String stableKey;
    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final OutputBehaviorType outputBehavior;

    @Override
    public String key() {
        return stableKey;
    }

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.PRACTICAL);
    }

    @Override
    public OutputBehaviorType getOutputBehavior() {
        return outputBehavior;
    }
}

