package org.example.sharedprompts.domain.prompt.common.enums.action.category.analysis;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum AnalysisActionType implements ActionTypeInterface, StableKeyedEnum {
    DATA_ANALYSIS("ACTION.ANALYSIS.DATA_ANALYSIS", "데이터 분석", "Data Analysis", "データ分析", OutputBehaviorType.ANALYTICAL_REPORT),
    STATISTICAL_ANALYSIS("ACTION.ANALYSIS.STATISTICAL_ANALYSIS", "통계 분석", "Statistical Analysis", "統計分析", OutputBehaviorType.ANALYTICAL_REPORT),
    INSIGHT_EXTRACTION("ACTION.ANALYSIS.INSIGHT_EXTRACTION", "인사이트 도출", "Insight Extraction", "インサイト抽出", OutputBehaviorType.ANALYTICAL_REPORT),
    TREND_ANALYSIS("ACTION.ANALYSIS.TREND_ANALYSIS", "트렌드 분석", "Trend Analysis", "トレンド分析", OutputBehaviorType.ANALYTICAL_REPORT),
    PATTERN_RECOGNITION("ACTION.ANALYSIS.PATTERN_RECOGNITION", "패턴 인식", "Pattern Recognition", "パターン認識", OutputBehaviorType.ANALYTICAL_REPORT),
    PREDICTIVE_ANALYSIS("ACTION.ANALYSIS.PREDICTIVE_ANALYSIS", "예측 분석", "Predictive Analysis", "予測分析", OutputBehaviorType.ANALYTICAL_REPORT),
    COMPARATIVE_ANALYSIS("ACTION.ANALYSIS.COMPARATIVE_ANALYSIS", "비교 분석", "Comparative Analysis", "比較分析", OutputBehaviorType.ANALYTICAL_REPORT),
    ROOT_CAUSE_ANALYSIS("ACTION.ANALYSIS.ROOT_CAUSE_ANALYSIS", "근본 원인 분석", "Root Cause Analysis", "根本原因分析", OutputBehaviorType.ANALYTICAL_REPORT),
    BUSINESS_INTELLIGENCE("ACTION.ANALYSIS.BUSINESS_INTELLIGENCE", "비즈니스 인텔리전스", "Business Intelligence", "ビジネスインテリジェンス", OutputBehaviorType.ANALYTICAL_REPORT);

    private final String stableKey;
    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final OutputBehaviorType outputBehavior;

    @Override
    public String key() {
        return stableKey;
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

