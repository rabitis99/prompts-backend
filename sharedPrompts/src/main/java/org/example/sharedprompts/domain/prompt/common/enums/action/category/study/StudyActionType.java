package org.example.sharedprompts.domain.prompt.common.enums.action.category.study;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Getter
@AllArgsConstructor
public enum StudyActionType implements ActionTypeInterface, StableKeyedEnum {
    STUDY_PLANNING("ACTION.STUDY.STUDY_PLANNING", "학습 계획", "Study Planning", "学習計画", OutputBehaviorType.EDUCATIONAL_EXPLANATION),
    NOTE_TAKING("ACTION.STUDY.NOTE_TAKING", "노트 작성", "Note Taking", "ノート作成", OutputBehaviorType.EDUCATIONAL_EXPLANATION),
    KNOWLEDGE_ORGANIZATION("ACTION.STUDY.KNOWLEDGE_ORGANIZATION", "지식 정리", "Knowledge Organization", "知識整理", OutputBehaviorType.EDUCATIONAL_EXPLANATION),
    COMPREHENSION_IMPROVEMENT("ACTION.STUDY.COMPREHENSION_IMPROVEMENT", "이해력 향상", "Comprehension Improvement", "理解力向上", OutputBehaviorType.EDUCATIONAL_EXPLANATION),
    MEMORIZATION_STRATEGY("ACTION.STUDY.MEMORIZATION_STRATEGY", "암기 전략", "Memorization Strategy", "暗記戦略", OutputBehaviorType.EDUCATIONAL_EXPLANATION),
    EXAM_PREPARATION("ACTION.STUDY.EXAM_PREPARATION", "시험 준비", "Exam Preparation", "試験準備", OutputBehaviorType.EDUCATIONAL_EXPLANATION),
    SKILL_DEVELOPMENT("ACTION.STUDY.SKILL_DEVELOPMENT", "기술 개발", "Skill Development", "スキル開発", OutputBehaviorType.EDUCATIONAL_EXPLANATION),
    LEARNING_PATH_DESIGN("ACTION.STUDY.LEARNING_PATH_DESIGN", "학습 경로 설계", "Learning Path Design", "学習パス設計", OutputBehaviorType.EDUCATIONAL_EXPLANATION),
    QUIZ_GENERATION("ACTION.STUDY.QUIZ_GENERATION", "퀴즈 생성", "Quiz Generation", "クイズ生成", OutputBehaviorType.EDUCATIONAL_EXPLANATION);

    private final String stableKey;
    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final OutputBehaviorType outputBehavior;

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

    @Override
    public OutputBehaviorType getOutputBehavior() {
        return outputBehavior;
    }
}

