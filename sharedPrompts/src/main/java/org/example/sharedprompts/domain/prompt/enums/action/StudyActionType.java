package org.example.sharedprompts.domain.prompt.enums.action;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StudyActionType implements ActionTypeInterface {
    STUDY_PLANNING("학습 계획", "Study Planning", "学習計画"),
    NOTE_TAKING("노트 작성", "Note Taking", "ノート作成"),
    KNOWLEDGE_ORGANIZATION("지식 정리", "Knowledge Organization", "知識整理"),
    COMPREHENSION_IMPROVEMENT("이해력 향상", "Comprehension Improvement", "理解力向上"),
    MEMORIZATION_STRATEGY("암기 전략", "Memorization Strategy", "暗記戦略"),
    EXAM_PREPARATION("시험 준비", "Exam Preparation", "試験準備"),
    SKILL_DEVELOPMENT("기술 개발", "Skill Development", "スキル開発"),
    LEARNING_PATH_DESIGN("학습 경로 설계", "Learning Path Design", "学習パス設計"),
    QUIZ_GENERATION("퀴즈 생성", "Quiz Generation", "クイズ生成");

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
}

