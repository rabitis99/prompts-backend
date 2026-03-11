package org.example.sharedprompts.domain.prompt.common.enums.action.category.creative;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum CreativeActionType implements ActionTypeInterface, StableKeyedEnum {
    IDEA_GENERATION("아이디어 생성", "Idea Generation", "アイデア生成"),
    CREATIVE_WRITING("창작 글쓰기", "Creative Writing", "創作執筆"),
    ARTISTIC_DESIGN("예술적 디자인", "Artistic Design", "芸術的デザイン"),
    STORYTELLING("스토리텔링", "Storytelling", "ストーリーテリング"),
    CONCEPT_DEVELOPMENT("컨셉 개발", "Concept Development", "コンセプト開発"),
    VISUAL_CREATION("시각적 창작", "Visual Creation", "視覚的創作"),
    CHARACTER_DEVELOPMENT("캐릭터 개발", "Character Development", "キャラクター開発"),
    WORLD_BUILDING("세계관 구축", "World Building", "世界観構築");

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;

    @Override
    public String key() {
        return "ACTION.CREATIVE." + name();
    }

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.CREATIVE);
    }
}

