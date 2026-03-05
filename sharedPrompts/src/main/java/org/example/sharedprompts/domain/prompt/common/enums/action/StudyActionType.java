package org.example.sharedprompts.domain.prompt.common.enums.action;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.enums.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Getter
@AllArgsConstructor
public enum StudyActionType implements ActionTypeInterface, StableKeyedEnum {
    STUDY_PLANNING("ACTION.STUDY.STUDY_PLANNING", "학습 계획", "Study Planning", "学習計画"),
    NOTE_TAKING("ACTION.STUDY.NOTE_TAKING", "노트 작성", "Note Taking", "ノート作成"),
    KNOWLEDGE_ORGANIZATION("ACTION.STUDY.KNOWLEDGE_ORGANIZATION", "지식 정리", "Knowledge Organization", "知識整理"),
    COMPREHENSION_IMPROVEMENT("ACTION.STUDY.COMPREHENSION_IMPROVEMENT", "이해력 향상", "Comprehension Improvement", "理解力向上"),
    MEMORIZATION_STRATEGY("ACTION.STUDY.MEMORIZATION_STRATEGY", "암기 전략", "Memorization Strategy", "暗記戦略"),
    EXAM_PREPARATION("ACTION.STUDY.EXAM_PREPARATION", "시험 준비", "Exam Preparation", "試験準備"),
    SKILL_DEVELOPMENT("ACTION.STUDY.SKILL_DEVELOPMENT", "기술 개발", "Skill Development", "スキル開発"),
    LEARNING_PATH_DESIGN("ACTION.STUDY.LEARNING_PATH_DESIGN", "학습 경로 설계", "Learning Path Design", "学習パス設計"),
    QUIZ_GENERATION("ACTION.STUDY.QUIZ_GENERATION", "퀴즈 생성", "Quiz Generation", "クイズ生成");

    private final String stableKey;
    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;

    static {
        Set<String> keys = new HashSet<>();
        Arrays.stream(values()).forEach(v -> {
            if (!keys.add(v.key())) {
                throw new IllegalStateException("Duplicate stableKey in StudyActionType: " + v.key());
            }
        });
    }

    @Override
    public String key() {
        return stableKey;
    }

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.EDUCATIONAL);
    }
}

