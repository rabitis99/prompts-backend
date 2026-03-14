package org.example.sharedprompts.domain.prompt.common.enums.action.category.creative;

import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

@Getter
public enum CreativeActionType implements ActionTypeInterface, StableKeyedEnum {
    IDEA_GENERATION("ACTION.CREATIVE.IDEA_GENERATION", "아이디어 생성", "Idea Generation", "アイデア生成", ActionGroup.CREATIVE_CONCEPT),
    CREATIVE_WRITING("ACTION.CREATIVE.CREATIVE_WRITING", "창작 글쓰기", "Creative Writing", "創作執筆", ActionGroup.CREATIVE_WRITING),
    ARTISTIC_DESIGN("ACTION.CREATIVE.ARTISTIC_DESIGN", "예술적 디자인", "Artistic Design", "芸術的デザイン", ActionGroup.GENERAL_DESIGN),
    STORYTELLING("ACTION.CREATIVE.STORYTELLING", "스토리텔링", "Storytelling", "ストーリーテリング", ActionGroup.CREATIVE_CONCEPT),
    CONCEPT_DEVELOPMENT("ACTION.CREATIVE.CONCEPT_DEVELOPMENT", "컨셉 개발", "Concept Development", "コンセプト開発", ActionGroup.CREATIVE_CONCEPT),
    VISUAL_CREATION("ACTION.CREATIVE.VISUAL_CREATION", "시각적 창작", "Visual Creation", "視覚的創作", ActionGroup.CREATIVE_CONCEPT),
    CHARACTER_DEVELOPMENT("ACTION.CREATIVE.CHARACTER_DEVELOPMENT", "캐릭터 개발", "Character Development", "キャラクター開発", ActionGroup.CREATIVE_CONCEPT),
    WORLD_BUILDING("ACTION.CREATIVE.WORLD_BUILDING", "세계관 구축", "World Building", "世界観構築", ActionGroup.CREATIVE_CONCEPT);

    private final String stableKey;
    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final ActionGroup actionGroup;

    CreativeActionType(String stableKey, String displayNameKo, String displayNameEn, String displayNameJa, ActionGroup actionGroup) {
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

