package org.example.sharedprompts.domain.prompt.common.enums.action.category.creative;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum CreativeActionType implements ActionTypeInterface, StableKeyedEnum {
    IDEA_GENERATION("아이디어 생성", "Idea Generation", "アイデア生成", OutputBehaviorType.LONG_FORM_WRITING),
    CREATIVE_WRITING("창작 글쓰기", "Creative Writing", "創作執筆", OutputBehaviorType.LONG_FORM_WRITING),
    ARTISTIC_DESIGN("예술적 디자인", "Artistic Design", "芸術的デザイン", OutputBehaviorType.LONG_FORM_WRITING),
    STORYTELLING("스토리텔링", "Storytelling", "ストーリーテリング", OutputBehaviorType.LONG_FORM_WRITING),
    CONCEPT_DEVELOPMENT("컨셉 개발", "Concept Development", "コンセプト開発", OutputBehaviorType.LONG_FORM_WRITING),
    VISUAL_CREATION("시각적 창작", "Visual Creation", "視覚的創作", OutputBehaviorType.LONG_FORM_WRITING),
    CHARACTER_DEVELOPMENT("캐릭터 개발", "Character Development", "キャラクター開発", OutputBehaviorType.LONG_FORM_WRITING),
    WORLD_BUILDING("세계관 구축", "World Building", "世界観構築", OutputBehaviorType.LONG_FORM_WRITING);

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final OutputBehaviorType outputBehavior;

    @Override
    public String key() {
        return "ACTION.CREATIVE." + name();
    }

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.CREATIVE);
    }

    @Override
    public OutputBehaviorType getOutputBehavior() {
        return outputBehavior;
    }
}

