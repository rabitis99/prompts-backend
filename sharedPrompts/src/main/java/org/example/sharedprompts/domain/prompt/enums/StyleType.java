package org.example.sharedprompts.domain.prompt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StyleType {
    NARRATIVE("서사형"),
    BULLET("글머리형"),
    CONCISE("간결형"),
    FORMATTED("서식형"),
    DESCRIPTIVE("묘사형"),
    INSTRUCTIVE("설명/가이드형"),
    QUESTION_ANSWER("질문-답변형"),
    STORYTELLING("스토리텔링형"),
    DIALOGUE("대화형"),
    COMPARATIVE("비교형"),
    ANALYTICAL("분석형");

    private final String description;
}
