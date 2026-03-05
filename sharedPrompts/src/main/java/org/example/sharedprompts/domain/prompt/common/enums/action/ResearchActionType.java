package org.example.sharedprompts.domain.prompt.common.enums.action;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.enums.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum ResearchActionType implements ActionTypeInterface, StableKeyedEnum {
    RESEARCH_DESIGN("연구 설계", "Research Design", "研究設計"),
    PAPER_WRITING("논문 작성", "Paper Writing", "論文執筆"),
    METHODOLOGY_DEVELOPMENT("방법론 개발", "Methodology Development", "方法論開発"),
    EXPERIMENT_DESIGN("실험 설계", "Experiment Design", "実験設計"),
    DATA_INTERPRETATION("데이터 해석", "Data Interpretation", "データ解釈"),
    LITERATURE_REVIEW("문헌 리뷰", "Literature Review", "文献レビュー"),
    HYPOTHESIS_FORMULATION("가설 수립", "Hypothesis Formulation", "仮説設定"),
    STATISTICAL_MODELING("통계 모델링", "Statistical Modeling", "統計モデリング");

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;

    @Override
    public String key() {
        return "ACTION.RESEARCH." + name();
    }

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.ANALYTICAL);
    }
}

