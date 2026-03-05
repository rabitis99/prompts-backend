package org.example.sharedprompts.domain.prompt.common.enums.action;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.enums.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum HealthFitnessActionType implements ActionTypeInterface, StableKeyedEnum {
    WORKOUT_PLANS("운동 계획", "Workout Plans", "ワークアウトプラン"),
    NUTRITION_TRACKING("영양 추적", "Nutrition Tracking", "栄養追跡"),
    MEDICAL_RECORD_MANAGEMENT("의료 기록 관리", "Medical Record Management", "医療記録管理"),
    FITNESS_GOAL_SETTING("피트니스 목표 설정", "Fitness Goal Setting", "フィットネス目標設定"),
    EXERCISE_PLANNING("운동 계획 수립", "Exercise Planning", "運動計画"),
    DIET_PLANNING("식단 계획", "Diet Planning", "食事計画"),
    WEIGHT_MANAGEMENT("체중 관리", "Weight Management", "体重管理"),
    CARDIO_TRAINING("유산소 운동", "Cardio Training", "有酸素運動"),
    STRENGTH_TRAINING("근력 운동", "Strength Training", "筋力トレーニング"),
    FLEXIBILITY_TRAINING("유연성 운동", "Flexibility Training", "柔軟性トレーニング"),
    RECOVERY_PLANNING("회복 계획", "Recovery Planning", "回復計画"),
    NUTRITION_PLANNING("영양 계획", "Nutrition Planning", "栄養計画"),
    MEAL_PREP("식사 준비", "Meal Prep", "食事準備"),
    SUPPLEMENT_GUIDANCE("보조제 가이드", "Supplement Guidance", "サプリメントガイド"),
    HEALTH_SCREENING("건강 검진", "Health Screening", "健康診断"),
    CHRONIC_DISEASE_MANAGEMENT("만성 질환 관리", "Chronic Disease Management", "慢性疾患管理"),
    MENTAL_HEALTH("정신 건강", "Mental Health", "メンタルヘルス"),
    SLEEP_OPTIMIZATION("수면 최적화", "Sleep Optimization", "睡眠最適化"),
    STRESS_MANAGEMENT_HEALTH("스트레스 관리", "Stress Management", "ストレス管理"),
    FITNESS_TRACKING("피트니스 추적", "Fitness Tracking", "フィットネス追跡"),
    WORKOUT_FORM_CORRECTION("운동 자세 교정", "Workout Form Correction", "ワークアウトフォーム修正");

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;

    @Override
    public String key() {
        return "ACTION.HEALTH_FITNESS." + name();
    }

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.PRACTICAL);
    }
}

