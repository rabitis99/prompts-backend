package org.example.sharedprompts.domain.prompt.common.enums.role.category.etc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;

@Getter
@AllArgsConstructor
public enum HealthFitnessRoleType implements RoleTypeInterface, StableKeyedEnum {
    FITNESS_COACH(
            "피트니스 코치",
            "운동 계획 수립 및 피트니스 목표 달성 지원 전문가",
            "Fitness Coach",
            "A specialist in workout planning and fitness goal achievement support",
            "フィットネスコーチ",
            "運動計画策定とフィットネス目標達成支援の専門家"
    ),
    NUTRITIONIST(
            "영양사",
            "영양 계획 및 식단 설계 전문가",
            "Nutritionist",
            "A specialist in nutrition planning and diet design",
            "栄養士",
            "栄養計画と食事設計の専門家"
    ),
    PERSONAL_TRAINER(
            "개인 트레이너",
            "개인 맞춤형 운동 프로그램 설계 및 지도 전문가",
            "Personal Trainer",
            "A specialist in personalized exercise program design and guidance",
            "パーソナルトレーナー",
            "個人カスタマイズ運動プログラム設計と指導の専門家"
    ),
    WELLNESS_COACH(
            "웰니스 코치",
            "건강 및 웰빙 라이프스타일 관리 전문가",
            "Wellness Coach",
            "A specialist in health and wellness lifestyle management",
            "ウェルネスコーチ",
            "健康とウェルビーイングライフスタイル管理の専門家"
    ),
    HEALTH_EDUCATOR(
            "건강 교육자",
            "건강 정보 제공 및 건강 관리 교육 전문가",
            "Health Educator",
            "A specialist in health information provision and health management education",
            "健康教育者",
            "健康情報提供と健康管理教育の専門家"
    );

    private final String roleNameKo;
    private final String descriptionKo;
    private final String roleNameEn;
    private final String descriptionEn;
    private final String roleNameJa;
    private final String descriptionJa;

    @Override
    public String keyPrefix() {
        return "ROLE.HEALTH_FITNESS";
    }

    @Override
    public String key() {
        return keyPrefix() + "." + name();
    }
}

