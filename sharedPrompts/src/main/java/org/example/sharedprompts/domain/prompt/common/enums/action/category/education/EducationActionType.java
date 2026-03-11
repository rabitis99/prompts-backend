package org.example.sharedprompts.domain.prompt.common.enums.action.category.education;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum EducationActionType implements ActionTypeInterface, StableKeyedEnum {
    CURRICULUM_DESIGN("ACTION.EDUCATION.CURRICULUM_DESIGN", "교육과정 설계", "Curriculum Design", "カリキュラム設計"),
    MATERIAL_CREATION("ACTION.EDUCATION.MATERIAL_CREATION", "교육 자료 생성", "Material Creation", "教材作成"),
    TEACHING_METHOD("ACTION.EDUCATION.TEACHING_METHOD", "교수법", "Teaching Method", "教授法"),
    LEARNER_ANALYSIS("ACTION.EDUCATION.LEARNER_ANALYSIS", "학습자 분석", "Learner Analysis", "学習者分析"),
    ASSESSMENT_DESIGN("ACTION.EDUCATION.ASSESSMENT_DESIGN", "평가 설계", "Assessment Design", "評価設計"),
    INTERACTIVE_CONTENT("ACTION.EDUCATION.INTERACTIVE_CONTENT", "인터랙티브 콘텐츠", "Interactive Content", "インタラクティブコンテンツ"),
    EDUCATIONAL_STRATEGY("ACTION.EDUCATION.EDUCATIONAL_STRATEGY", "교육 전략", "Educational Strategy", "教育戦略"),
    LESSON_PLANNING("ACTION.EDUCATION.LESSON_PLANNING", "수업 계획", "Lesson Planning", "授業計画");

    private final String stableKey;
    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;

    @Override
    public String key() {
        return stableKey;
    }

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.EDUCATIONAL);
    }
}

