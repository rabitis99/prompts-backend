package org.example.sharedprompts.domain.prompt.common.enums.action.category.research;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

@Getter
@AllArgsConstructor
public enum ResearchActionType implements ActionTypeInterface, StableKeyedEnum {
    RESEARCH_DESIGN("연구 설계", "Research Design", "研究設計", ActionGroup.RESEARCH_METHODOLOGY),
    PAPER_WRITING("논문 작성", "Paper Writing", "論文執筆", ActionGroup.LONG_FORM_WRITING),
    METHODOLOGY_DEVELOPMENT("방법론 개발", "Methodology Development", "方法論開発", ActionGroup.RESEARCH_METHODOLOGY),
    EXPERIMENT_DESIGN("실험 설계", "Experiment Design", "実験設計", ActionGroup.RESEARCH_METHODOLOGY),
    DATA_INTERPRETATION("데이터 해석", "Data Interpretation", "データ解釈", ActionGroup.DATA_ANALYSIS),
    LITERATURE_REVIEW("문헌 리뷰", "Literature Review", "文献レビュー", ActionGroup.RESEARCH_METHODOLOGY),
    HYPOTHESIS_FORMULATION("가설 수립", "Hypothesis Formulation", "仮説設定", ActionGroup.RESEARCH_METHODOLOGY),
    STATISTICAL_MODELING("통계 모델링", "Statistical Modeling", "統計モデリング", ActionGroup.DATA_ANALYSIS);

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final ActionGroup actionGroup;

    @Override
    public String key() {
        return "ACTION.RESEARCH." + name();
    }

    @Override
    public ActionGroup getActionGroup() {
        return actionGroup;
    }
}

