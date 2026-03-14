package org.example.sharedprompts.domain.prompt.common.enums.action.category.etc;

import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

@Getter
public enum HealthFitnessActionType implements ActionTypeInterface, StableKeyedEnum {
    WORKOUT_PLANS("ACTION.HEALTH_FITNESS.WORKOUT_PLANS", "운동 계획", "Workout Plans", "ワークアウトプラン", ActionGroup.WORKOUT_PLANNING),
    NUTRITION_TRACKING("ACTION.HEALTH_FITNESS.NUTRITION_TRACKING", "영양 추적", "Nutrition Tracking", "栄養追跡", ActionGroup.NUTRITION_GUIDANCE),
    MEDICAL_RECORD_MANAGEMENT("ACTION.HEALTH_FITNESS.MEDICAL_RECORD_MANAGEMENT", "의료 기록 관리", "Medical Record Management", "医療記録管理", ActionGroup.HEALTH_TRACKING),
    FITNESS_GOAL_SETTING("ACTION.HEALTH_FITNESS.FITNESS_GOAL_SETTING", "피트니스 목표 설정", "Fitness Goal Setting", "フィットネス目標設定", ActionGroup.WORKOUT_PLANNING),
    EXERCISE_PLANNING("ACTION.HEALTH_FITNESS.EXERCISE_PLANNING", "운동 계획 수립", "Exercise Planning", "運動計画", ActionGroup.WORKOUT_PLANNING),
    DIET_PLANNING("ACTION.HEALTH_FITNESS.DIET_PLANNING", "식단 계획", "Diet Planning", "食事計画", ActionGroup.NUTRITION_GUIDANCE),
    WEIGHT_MANAGEMENT("ACTION.HEALTH_FITNESS.WEIGHT_MANAGEMENT", "체중 관리", "Weight Management", "体重管理", ActionGroup.HEALTH_TRACKING),
    CARDIO_TRAINING("ACTION.HEALTH_FITNESS.CARDIO_TRAINING", "유산소 운동", "Cardio Training", "有酸素運動", ActionGroup.WORKOUT_PLANNING),
    STRENGTH_TRAINING("ACTION.HEALTH_FITNESS.STRENGTH_TRAINING", "근력 운동", "Strength Training", "筋力トレーニング", ActionGroup.WORKOUT_PLANNING),
    FLEXIBILITY_TRAINING("ACTION.HEALTH_FITNESS.FLEXIBILITY_TRAINING", "유연성 운동", "Flexibility Training", "柔軟性トレーニング", ActionGroup.WORKOUT_PLANNING),
    RECOVERY_PLANNING("ACTION.HEALTH_FITNESS.RECOVERY_PLANNING", "회복 계획", "Recovery Planning", "回復計画", ActionGroup.WORKOUT_PLANNING),
    NUTRITION_PLANNING("ACTION.HEALTH_FITNESS.NUTRITION_PLANNING", "영양 계획", "Nutrition Planning", "栄養計画", ActionGroup.NUTRITION_GUIDANCE),
    MEAL_PREP("ACTION.HEALTH_FITNESS.MEAL_PREP", "식사 준비", "Meal Prep", "食事準備", ActionGroup.NUTRITION_GUIDANCE),
    SUPPLEMENT_GUIDANCE("ACTION.HEALTH_FITNESS.SUPPLEMENT_GUIDANCE", "보조제 가이드", "Supplement Guidance", "サプリメントガイド", ActionGroup.NUTRITION_GUIDANCE),
    HEALTH_SCREENING("ACTION.HEALTH_FITNESS.HEALTH_SCREENING", "건강 검진", "Health Screening", "健康診断", ActionGroup.HEALTH_TRACKING),
    CHRONIC_DISEASE_MANAGEMENT("ACTION.HEALTH_FITNESS.CHRONIC_DISEASE_MANAGEMENT", "만성 질환 관리", "Chronic Disease Management", "慢性疾患管理", ActionGroup.HEALTH_TRACKING),
    MENTAL_HEALTH("ACTION.HEALTH_FITNESS.MENTAL_HEALTH", "정신 건강", "Mental Health", "メンタルヘルス", ActionGroup.MENTAL_WELLNESS),
    SLEEP_OPTIMIZATION("ACTION.HEALTH_FITNESS.SLEEP_OPTIMIZATION", "수면 최적화", "Sleep Optimization", "睡眠最適化", ActionGroup.MENTAL_WELLNESS),
    STRESS_MANAGEMENT_HEALTH("ACTION.HEALTH_FITNESS.STRESS_MANAGEMENT_HEALTH", "스트레스 관리", "Stress Management", "ストレス管理", ActionGroup.MENTAL_WELLNESS),
    FITNESS_TRACKING("ACTION.HEALTH_FITNESS.FITNESS_TRACKING", "피트니스 추적", "Fitness Tracking", "フィットネス追跡", ActionGroup.HEALTH_TRACKING),
    WORKOUT_FORM_CORRECTION("ACTION.HEALTH_FITNESS.WORKOUT_FORM_CORRECTION", "운동 자세 교정", "Workout Form Correction", "ワークアウトフォーム修正", ActionGroup.WORKOUT_PLANNING);

    private final String stableKey;
    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final ActionGroup actionGroup;

    HealthFitnessActionType(String stableKey, String displayNameKo, String displayNameEn, String displayNameJa, ActionGroup actionGroup) {
        this.stableKey = stableKey;
        this.displayNameKo = displayNameKo;
        this.displayNameEn = displayNameEn;
        this.displayNameJa = displayNameJa;
        this.actionGroup = actionGroup;
    }

    @Override
    public String key() {
        return stableKey;
    }

    @Override
    public ActionGroup getActionGroup() {
        return actionGroup;
    }
}

