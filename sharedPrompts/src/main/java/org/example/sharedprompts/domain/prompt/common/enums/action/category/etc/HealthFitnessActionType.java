package org.example.sharedprompts.domain.prompt.common.enums.action.category.etc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum HealthFitnessActionType implements ActionTypeInterface, StableKeyedEnum {
    WORKOUT_PLANS("운동 계획", "Workout Plans", "ワークアウトプラン", OutputBehaviorType.STRATEGIC_PLAN),
    NUTRITION_TRACKING("영양 추적", "Nutrition Tracking", "栄養追跡", OutputBehaviorType.STRATEGIC_PLAN),
    MEDICAL_RECORD_MANAGEMENT("의료 기록 관리", "Medical Record Management", "医療記録管理", OutputBehaviorType.STRATEGIC_PLAN),
    FITNESS_GOAL_SETTING("피트니스 목표 설정", "Fitness Goal Setting", "フィットネス目標設定", OutputBehaviorType.STRATEGIC_PLAN),
    EXERCISE_PLANNING("운동 계획 수립", "Exercise Planning", "運動計画", OutputBehaviorType.STRATEGIC_PLAN),
    DIET_PLANNING("식단 계획", "Diet Planning", "食事計画", OutputBehaviorType.STRATEGIC_PLAN),
    WEIGHT_MANAGEMENT("체중 관리", "Weight Management", "体重管理", OutputBehaviorType.STRATEGIC_PLAN),
    CARDIO_TRAINING("유산소 운동", "Cardio Training", "有酸素運動", OutputBehaviorType.STRATEGIC_PLAN),
    STRENGTH_TRAINING("근력 운동", "Strength Training", "筋力トレーニング", OutputBehaviorType.STRATEGIC_PLAN),
    FLEXIBILITY_TRAINING("유연성 운동", "Flexibility Training", "柔軟性トレーニング", OutputBehaviorType.STRATEGIC_PLAN),
    RECOVERY_PLANNING("회복 계획", "Recovery Planning", "回復計画", OutputBehaviorType.STRATEGIC_PLAN),
    NUTRITION_PLANNING("영양 계획", "Nutrition Planning", "栄養計画", OutputBehaviorType.STRATEGIC_PLAN),
    MEAL_PREP("식사 준비", "Meal Prep", "食事準備", OutputBehaviorType.STRATEGIC_PLAN),
    SUPPLEMENT_GUIDANCE("보조제 가이드", "Supplement Guidance", "サプリメントガイド", OutputBehaviorType.STRATEGIC_PLAN),
    HEALTH_SCREENING("건강 검진", "Health Screening", "健康診断", OutputBehaviorType.STRATEGIC_PLAN),
    CHRONIC_DISEASE_MANAGEMENT("만성 질환 관리", "Chronic Disease Management", "慢性疾患管理", OutputBehaviorType.STRATEGIC_PLAN),
    MENTAL_HEALTH("정신 건강", "Mental Health", "メンタルヘルス", OutputBehaviorType.STRATEGIC_PLAN),
    SLEEP_OPTIMIZATION("수면 최적화", "Sleep Optimization", "睡眠最適化", OutputBehaviorType.STRATEGIC_PLAN),
    STRESS_MANAGEMENT_HEALTH("스트레스 관리", "Stress Management", "ストレス管理", OutputBehaviorType.STRATEGIC_PLAN),
    FITNESS_TRACKING("피트니스 추적", "Fitness Tracking", "フィットネス追跡", OutputBehaviorType.STRATEGIC_PLAN),
    WORKOUT_FORM_CORRECTION("운동 자세 교정", "Workout Form Correction", "ワークアウトフォーム修正", OutputBehaviorType.STRATEGIC_PLAN);

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final OutputBehaviorType outputBehavior;

    @Override
    public String key() {
        return "ACTION.HEALTH_FITNESS." + name();
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

