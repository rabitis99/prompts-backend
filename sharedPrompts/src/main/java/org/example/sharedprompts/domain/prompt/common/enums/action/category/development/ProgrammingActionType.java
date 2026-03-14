package org.example.sharedprompts.domain.prompt.common.enums.action.category.development;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

@Getter
@AllArgsConstructor
public enum ProgrammingActionType implements ActionTypeInterface, StableKeyedEnum {
    ALGORITHM_IMPLEMENTATION("알고리즘 구현", "Algorithm Implementation", "アルゴリズム実装", ActionGroup.CODE_GENERATION),
    DATA_STRUCTURE_DESIGN("자료구조 설계", "Data Structure Design", "データ構造設計", ActionGroup.CODE_GENERATION),
    LANGUAGE_LEARNING("언어 학습", "Language Learning", "言語学習", ActionGroup.EXPLANATION),
    SYNTAX_OPTIMIZATION("문법 최적화", "Syntax Optimization", "構文最適化", ActionGroup.CODE_MODIFICATION),
    LOGIC_DEVELOPMENT("로직 개발", "Logic Development", "ロジック開発", ActionGroup.CODE_GENERATION),
    API_DESIGN("API 설계", "API Design", "API設計", ActionGroup.CODE_GENERATION),
    CONCURRENT_PROGRAMMING("동시성 프로그래밍", "Concurrent Programming", "並行プログラミング", ActionGroup.CODE_GENERATION);

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final ActionGroup actionGroup;

    @Override
    public String key() {
        return "ACTION.PROGRAMMING." + name();
    }

    @Override
    public ActionGroup getActionGroup() {
        return actionGroup;
    }
}

