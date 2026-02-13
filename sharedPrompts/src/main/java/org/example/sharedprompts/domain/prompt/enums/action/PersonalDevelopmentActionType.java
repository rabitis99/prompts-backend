package org.example.sharedprompts.domain.prompt.enums.action;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;

import java.util.Optional;

/**
 * 자기계발 관련 액션 타입 enum
 * 
 * <p>생성자 파라미터 순서 (모든 파라미터는 String 타입):
 * <ol>
 *   <li>displayNameKo - 표시 이름 (한국어)</li>
 *   <li>displayNameEn - 표시 이름 (영어)</li>
 *   <li>displayNameJa - 표시 이름 (일본어)</li>
 * </ol>
 */
@Getter
@AllArgsConstructor
public enum PersonalDevelopmentActionType implements ActionTypeInterface {
    GOAL_SETTING("목표 설정", "Goal Setting", "目標設定"),
    HABIT_FORMATION("습관 형성", "Habit Formation", "習慣形成"),
    SELF_IMPROVEMENT("자기계발", "Self Improvement", "自己啓発"),
    STRESS_MANAGEMENT("스트레스 관리", "Stress Management", "ストレス管理"),
    MOTIVATION("동기 부여", "Motivation", "モチベーション"),
    DECISION_MAKING("의사결정", "Decision Making", "意思決定");

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.EDUCATIONAL);
    }
}

