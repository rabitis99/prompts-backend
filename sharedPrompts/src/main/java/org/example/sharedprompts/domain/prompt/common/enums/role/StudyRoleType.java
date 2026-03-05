package org.example.sharedprompts.domain.prompt.common.enums.role;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.enums.StableKeyedEnum;

@Getter
@AllArgsConstructor
public enum StudyRoleType implements RoleTypeInterface, StableKeyedEnum {
    STUDY_COACH(
            "학습 코치",
            "효율적인 학습 방법 제시 및 학습 계획 수립 전문가",
            "Study Coach",
            "A specialist who provides effective study methods and creates study plans",
            "学習コーチ",
            "効率的な学習方法提示と学習計画策定の専門家"
    ),
    LEARNING_SPECIALIST(
            "학습 전문가",
            "학습 전략 개발 및 지식 정리 방법론 전문가",
            "Learning Specialist",
            "A specialist in learning strategy development and knowledge organization methodologies",
            "学習専門家",
            "学習戦略開発と知識整理方法論の専門家"
    ),
    TUTOR(
            "튜터",
            "개인 맞춤형 학습 지도 및 이해력 향상 지원 전문가",
            "Tutor",
            "A specialist who provides personalized learning guidance and supports comprehension improvement",
            "チューター",
            "個人カスタマイズ学習指導と理解力向上支援の専門家"
    );

    private final String roleNameKo;
    private final String descriptionKo;
    private final String roleNameEn;
    private final String descriptionEn;
    private final String roleNameJa;
    private final String descriptionJa;

    @Override
    public String key() {
        return "ROLE.STUDY." + name();
    }
}

