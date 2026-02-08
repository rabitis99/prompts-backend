package org.example.sharedprompts.domain.prompt.enums.action;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AnalysisActionType implements ActionTypeInterface {
    DATA_ANALYSIS("데이터 분석", "Data Analysis", "データ分析"),
    STATISTICAL_ANALYSIS("통계 분석", "Statistical Analysis", "統計分析"),
    INSIGHT_EXTRACTION("인사이트 도출", "Insight Extraction", "インサイト抽出"),
    TREND_ANALYSIS("트렌드 분석", "Trend Analysis", "トレンド分析"),
    PATTERN_RECOGNITION("패턴 인식", "Pattern Recognition", "パターン認識"),
    PREDICTIVE_ANALYSIS("예측 분석", "Predictive Analysis", "予測分析"),
    COMPARATIVE_ANALYSIS("비교 분석", "Comparative Analysis", "比較分析"),
    ROOT_CAUSE_ANALYSIS("근본 원인 분석", "Root Cause Analysis", "根本原因分析"),
    BUSINESS_INTELLIGENCE("비즈니스 인텔리전스", "Business Intelligence", "ビジネスインテリジェンス");

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
}

