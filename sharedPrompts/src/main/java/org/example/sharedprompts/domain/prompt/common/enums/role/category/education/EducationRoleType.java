package org.example.sharedprompts.domain.prompt.common.enums.role.category.education;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;

@Getter
@AllArgsConstructor
public enum EducationRoleType implements RoleTypeInterface {
    EDUCATOR(
            "교육자",
            "교육 자료 설계 및 효과적인 교수법 적용 전문가",
            "Educator",
            "A specialist in educational material design and effective teaching method application",
            "教育者",
            "教育資料設計と効果的な教授法適用の専門家"
    ),
    CURRICULUM_DESIGNER(
            "교육과정 설계자",
            "교육과정 개발 및 학습자 맞춤형 교육 전략 수립 전문가",
            "Curriculum Designer",
            "A specialist in curriculum development and learner-customized educational strategy planning",
            "カリキュラム設計者",
            "教育課程開発と学習者カスタマイズ教育戦略策定の専門家"
    ),
    INSTRUCTIONAL_DESIGNER(
            "교수 설계자",
            "학습 목표 달성을 위한 교수 설계 및 평가 체계 구축 전문가",
            "Instructional Designer",
            "A specialist in instructional design and assessment system construction for achieving learning objectives",
            "教授設計者",
            "学習目標達成のための教授設計と評価体系構築の専門家"
    );

    private final String roleNameKo;
    private final String descriptionKo;
    private final String roleNameEn;
    private final String descriptionEn;
    private final String roleNameJa;
    private final String descriptionJa;

    @Override
    public String keyPrefix() {
        return "ROLE.EDUCATION";
    }
}

