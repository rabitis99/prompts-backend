package org.example.sharedprompts.domain.prompt.enums.action;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum EducationActionType implements ActionTypeInterface {
    CURRICULUM_DESIGN("교육과정 설계", "Curriculum Design", "カリキュラム設計"),
    MATERIAL_CREATION("교육 자료 생성", "Material Creation", "教材作成"),
    TEACHING_METHOD("교수법", "Teaching Method", "教授法"),
    LEARNER_ANALYSIS("학습자 분석", "Learner Analysis", "学習者分析"),
    ASSESSMENT_DESIGN("평가 설계", "Assessment Design", "評価設計"),
    INTERACTIVE_CONTENT("인터랙티브 콘텐츠", "Interactive Content", "インタラクティブコンテンツ"),
    EDUCATIONAL_STRATEGY("교육 전략", "Educational Strategy", "教育戦略"),
    LESSON_PLANNING("수업 계획", "Lesson Planning", "授業計画");

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.EDUCATIONAL);
    }
}

