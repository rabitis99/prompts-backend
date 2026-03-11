package org.example.sharedprompts.domain.prompt.common.enums.role.category.creative;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;

@Getter
@AllArgsConstructor
public enum CreativeRoleType implements RoleTypeInterface, StableKeyedEnum {
    CREATIVE_DIRECTOR(
            "크리에이티브 디렉터",
            "창의적 아이디어 개발 및 창작물 기획 전문가",
            "Creative Director",
            "A specialist in developing creative ideas and planning creative works",
            "クリエイティブディレクター",
            "創造的アイデア開発と創作物企画の専門家"
    ),
    STORYTELLER(
            "스토리텔러",
            "스토리 구성 및 스토리텔링 기법 전문가",
            "Storyteller",
            "A specialist in story composition and storytelling techniques",
            "ストーリーテラー",
            "ストーリー構成とストーリーテリング技法の専門家"
    ),
    CONCEPT_ARTIST(
            "컨셉 아티스트",
            "시각적 컨셉 개발 및 예술적 디자인 전문가",
            "Concept Artist",
            "A specialist in visual concept development and artistic design",
            "コンセプトアーティスト",
            "視覚的コンセプト開発と芸術的デザインの専門家"
    );

    private final String roleNameKo;
    private final String descriptionKo;
    private final String roleNameEn;
    private final String descriptionEn;
    private final String roleNameJa;
    private final String descriptionJa;

    @Override
    public String keyPrefix() {
        return "ROLE.CREATIVE";
    }
}

