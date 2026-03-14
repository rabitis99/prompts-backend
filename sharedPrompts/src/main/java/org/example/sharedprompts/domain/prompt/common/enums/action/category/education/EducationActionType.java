package org.example.sharedprompts.domain.prompt.common.enums.action.category.education;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

@Getter
@AllArgsConstructor
public enum EducationActionType implements ActionTypeInterface, StableKeyedEnum {
    CURRICULUM_DESIGN("ACTION.EDUCATION.CURRICULUM_DESIGN", "교육과정 설계", "Curriculum Design", "カリキュラム設計", ActionGroup.EDUCATION_DESIGN),
    MATERIAL_CREATION("ACTION.EDUCATION.MATERIAL_CREATION", "교육 자료 생성", "Material Creation", "教材作成", ActionGroup.EDUCATION_DESIGN),
    TEACHING_METHOD("ACTION.EDUCATION.TEACHING_METHOD", "교수법", "Teaching Method", "教授法", ActionGroup.EDUCATION_DESIGN),
    LEARNER_ANALYSIS("ACTION.EDUCATION.LEARNER_ANALYSIS", "학습자 분석", "Learner Analysis", "学習者分析", ActionGroup.EDUCATION_DESIGN),
    ASSESSMENT_DESIGN("ACTION.EDUCATION.ASSESSMENT_DESIGN", "평가 설계", "Assessment Design", "評価設計", ActionGroup.EDUCATION_DESIGN),
    INTERACTIVE_CONTENT("ACTION.EDUCATION.INTERACTIVE_CONTENT", "인터랙티브 콘텐츠", "Interactive Content", "インタラクティブコンテンツ", ActionGroup.EDUCATION_DESIGN),
    EDUCATIONAL_STRATEGY("ACTION.EDUCATION.EDUCATIONAL_STRATEGY", "교육 전략", "Educational Strategy", "教育戦略", ActionGroup.EDUCATION_DESIGN),
    LESSON_PLANNING("ACTION.EDUCATION.LESSON_PLANNING", "수업 계획", "Lesson Planning", "授業計画", ActionGroup.EDUCATION_DESIGN);

    private final String stableKey;
    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final ActionGroup actionGroup;

    @Override
    public String key() {
        return stableKey;
    }

    @Override
    public ActionGroup getActionGroup() {
        return actionGroup;
    }
}

