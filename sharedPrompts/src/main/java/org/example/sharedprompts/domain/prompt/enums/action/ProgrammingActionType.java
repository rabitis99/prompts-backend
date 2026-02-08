package org.example.sharedprompts.domain.prompt.enums.action;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProgrammingActionType implements ActionTypeInterface {
    ALGORITHM_IMPLEMENTATION("알고리즘 구현", "Algorithm Implementation", "アルゴリズム実装"),
    DATA_STRUCTURE_DESIGN("자료구조 설계", "Data Structure Design", "データ構造設計"),
    LANGUAGE_LEARNING("언어 학습", "Language Learning", "言語学習"),
    SYNTAX_OPTIMIZATION("문법 최적화", "Syntax Optimization", "構文最適化"),
    LOGIC_DEVELOPMENT("로직 개발", "Logic Development", "ロジック開発"),
    API_DESIGN("API 설계", "API Design", "API設計"),
    CONCURRENT_PROGRAMMING("동시성 프로그래밍", "Concurrent Programming", "並行プログラミング");

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
}

