package org.example.sharedprompts.domain.prompt.common.enums.action.category.research;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum ResearchActionType implements ActionTypeInterface, StableKeyedEnum {
    RESEARCH_DESIGN("연구 설계", "Research Design", "研究設計", OutputBehaviorType.ANALYTICAL_REPORT),
    PAPER_WRITING("논문 작성", "Paper Writing", "論文執筆", OutputBehaviorType.ANALYTICAL_REPORT),
    METHODOLOGY_DEVELOPMENT("방법론 개발", "Methodology Development", "方法論開発", OutputBehaviorType.ANALYTICAL_REPORT),
    EXPERIMENT_DESIGN("실험 설계", "Experiment Design", "実験設計", OutputBehaviorType.ANALYTICAL_REPORT),
    DATA_INTERPRETATION("데이터 해석", "Data Interpretation", "データ解釈", OutputBehaviorType.ANALYTICAL_REPORT),
    LITERATURE_REVIEW("문헌 리뷰", "Literature Review", "文献レビュー", OutputBehaviorType.ANALYTICAL_REPORT),
    HYPOTHESIS_FORMULATION("가설 수립", "Hypothesis Formulation", "仮説設定", OutputBehaviorType.ANALYTICAL_REPORT),
    STATISTICAL_MODELING("통계 모델링", "Statistical Modeling", "統計モデリング", OutputBehaviorType.ANALYTICAL_REPORT);

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final OutputBehaviorType outputBehavior;

    @Override
    public String key() {
        return "ACTION.RESEARCH." + name();
    }

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.ANALYTICAL);
    }

    @Override
    public OutputBehaviorType getOutputBehavior() {
        return outputBehavior;
    }
}

