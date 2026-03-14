package org.example.sharedprompts.domain.prompt.common.enums.action.category.productivity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

/** 자기계발 관련 액션 타입 enum */
@Getter
@AllArgsConstructor
public enum PersonalDevelopmentActionType implements ActionTypeInterface, StableKeyedEnum {
    GOAL_SETTING("ACTION.PERSONAL_DEVELOPMENT.GOAL_SETTING", "목표 설정", "Goal Setting", "目標設定", ActionGroup.PERSONAL_PRODUCTIVITY),
    HABIT_FORMATION("ACTION.PERSONAL_DEVELOPMENT.HABIT_FORMATION", "습관 형성", "Habit Formation", "習慣形成", ActionGroup.PERSONAL_PRODUCTIVITY),
    SELF_IMPROVEMENT("ACTION.PERSONAL_DEVELOPMENT.SELF_IMPROVEMENT", "자기계발", "Self Improvement", "自己啓発", ActionGroup.PERSONAL_PRODUCTIVITY),
    STRESS_MANAGEMENT("ACTION.PERSONAL_DEVELOPMENT.STRESS_MANAGEMENT", "스트레스 관리", "Stress Management", "ストレス管理", ActionGroup.MENTAL_WELLNESS),
    MOTIVATION("ACTION.PERSONAL_DEVELOPMENT.MOTIVATION", "동기 부여", "Motivation", "モチベーション", ActionGroup.PERSONAL_PRODUCTIVITY),
    DECISION_MAKING("ACTION.PERSONAL_DEVELOPMENT.DECISION_MAKING", "의사결정", "Decision Making", "意思決定", ActionGroup.PERSONAL_PRODUCTIVITY);

    private final String stableKey;
    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final ActionGroup actionGroup;

    @Override
    public String key() {
        return stableKey;
    }

    @Override
    public ActionGroup getActionGroup() {
        return actionGroup;
    }
}

