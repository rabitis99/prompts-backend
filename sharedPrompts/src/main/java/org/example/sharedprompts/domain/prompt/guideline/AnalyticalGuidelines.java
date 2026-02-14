package org.example.sharedprompts.domain.prompt.guideline;

import java.util.List;

import static org.example.sharedprompts.domain.prompt.guideline.RuleLevel.*;
import static org.example.sharedprompts.domain.prompt.guideline.RuleType.*;

/**
 * ANALYTICAL 도메인 가이드라인 — 증거 기반 체계적 분석
 */
public final class AnalyticalGuidelines implements GuidelinePolicy {

    public static final AnalyticalGuidelines INSTANCE = new AnalyticalGuidelines();

    private static final List<GuidelineRule> PRINCIPLES = List.of(
            new GuidelineRule(
                    "ANALYTICAL.PRINCIPLE.EVIDENCE_BASED",
                    I18nText.of("증거 기반", "Evidence-Based", "証拠ベース"),
                    I18nText.of(
                            "모든 주장과 결론은 데이터·사례·논거로 뒷받침. 근거 없는 주장이나 개인적 의견 배제",
                            "Support all claims and conclusions with data, cases, and arguments. Exclude unsubstantiated claims or personal opinions",
                            "すべての主張と結論はデータ・事例・論拠で裏付け。根拠のない主張や個人的意見を排除"
                    ),
                    HARD, REQUIRE
            ),
            new GuidelineRule(
                    "ANALYTICAL.PRINCIPLE.MULTI_PERSPECTIVE",
                    I18nText.of("다각적 관점", "Multi-Perspective", "多角的視点"),
                    I18nText.of(
                            "단일 시각에 치우치지 않고 복수의 관점에서 현상 분석. 상반된 의견이나 대안적 해석도 함께 제시",
                            "Analyze phenomena from multiple perspectives without bias. Present opposing views and alternative interpretations",
                            "単一の視角に偏らず複数の観点から現象を分析。相反する意見や代替的な解釈も併せて提示"
                    ),
                    HARD, REQUIRE
            ),
            new GuidelineRule(
                    "ANALYTICAL.PRINCIPLE.SYSTEMATIC",
                    I18nText.of("체계적 사고", "Systematic Thinking", "体系的思考"),
                    I18nText.of(
                            "분석 과정을 명확한 프레임워크와 방법론에 따라 수행. 가설→검증→결론의 논리적 흐름 유지",
                            "Conduct analysis following clear frameworks and methodologies. Maintain logical flow of hypothesis → verification → conclusion",
                            "分析過程を明確なフレームワークと方法論に従って実行。仮説→検証→結論の論理的な流れを維持"
                    ),
                    SOFT, REQUIRE
            )
    );

    private static final List<GuidelineRule> STRUCTURING_RULES = List.of(
            new GuidelineRule(
                    "ANALYTICAL.STRUCTURE.LOGICAL",
                    I18nText.of("논리적 구조", "Logical Structure", "論理的構造"),
                    I18nText.of(
                            "서론(배경/목적) → 본론(분석/근거) → 결론(종합/제언)의 명확한 구조",
                            "Clear structure of introduction (background/purpose) → body (analysis/evidence) → conclusion (synthesis/recommendations)",
                            "序論（背景/目的） → 本論（分析/根拠） → 結論（総合/提言）の明確な構造"
                    ),
                    HARD, REQUIRE
            ),
            new GuidelineRule(
                    "ANALYTICAL.STRUCTURE.DATA_VISUALIZATION",
                    I18nText.of("데이터 시각화", "Data Visualization", "データ可視化"),
                    I18nText.of(
                            "수치·비교·추이는 표·차트 설명·요약 통계를 활용하여 제시",
                            "Present numbers, comparisons, and trends using tables, chart descriptions, and summary statistics",
                            "数値・比較・推移は表・チャート説明・要約統計を活用して提示"
                    ),
                    SOFT, REQUIRE
            ),
            new GuidelineRule(
                    "ANALYTICAL.STRUCTURE.CAUSALITY",
                    I18nText.of("인과관계 명시", "Explicit Causality", "因果関係の明示"),
                    I18nText.of(
                            "원인과 결과의 관계를 명확히 구분, 상관관계와 혼동 금지",
                            "Clearly distinguish cause-and-effect relationships. Never confuse correlation with causation",
                            "原因と結果の関係を明確に区分、相関関係との混同を禁止"
                    ),
                    HARD, REQUIRE
            ),
            new GuidelineRule(
                    "ANALYTICAL.STRUCTURE.LIMITATIONS",
                    I18nText.of("한계 인정", "Acknowledge Limitations", "限界の認識"),
                    I18nText.of(
                            "분석의 제한사항, 데이터의 한계, 추가 검증 필요 영역을 솔직히 명시",
                            "Honestly state analysis limitations, data constraints, and areas requiring additional verification",
                            "分析の制限事項、データの限界、追加検証が必要な領域を正直に明示"
                    ),
                    SOFT, REQUIRE
            )
    );

    private static final List<GuidelineRule> QUALITY_STANDARDS = List.of(
            new GuidelineRule(
                    "ANALYTICAL.QUALITY.RIGOR",
                    I18nText.of("논리적 엄밀성", "Logical Rigor", "論理的厳密性"),
                    I18nText.of(
                            "추론 과정에 논리적 비약이나 오류가 없어야 함. 전제→결론의 논증이 타당해야 함",
                            "Reasoning must be free of logical leaps or errors. The argument from premises to conclusion must be valid",
                            "推論過程に論理的な飛躍や誤りがあってはならない。前提→結論の論証が妥当でなければならない"
                    ),
                    HARD, REQUIRE
            ),
            new GuidelineRule(
                    "ANALYTICAL.QUALITY.OBJECTIVITY",
                    I18nText.of("객관성", "Objectivity", "客観性"),
                    I18nText.of(
                            "편향 없는 중립적 분석 수행. 사실과 해석을 명확히 구분",
                            "Conduct unbiased, neutral analysis. Clearly distinguish facts from interpretations",
                            "偏りのない中立的な分析を実行。事実と解釈を明確に区分"
                    ),
                    HARD, REQUIRE
            ),
            new GuidelineRule(
                    "ANALYTICAL.QUALITY.ACTIONABILITY",
                    I18nText.of("실행 가능성", "Actionability", "実行可能性"),
                    I18nText.of(
                            "분석 결과에서 구체적 행동 방안이나 의사결정 근거를 도출. 단순 현황 기술에 그치지 않음",
                            "Derive specific action plans or decision-making bases from analysis results. Go beyond mere description of current state",
                            "分析結果から具体的な行動方針や意思決定の根拠を導出。単純な現況記述に留まらない"
                    ),
                    SOFT, REQUIRE
            )
    );

    private static final List<GuidelineRule> OUTPUT_CONSTRAINTS = List.of(
            new GuidelineRule(
                    "ANALYTICAL.OUTPUT.NO_GREETING",
                    I18nText.of("인사말 금지", "No Greetings", "挨拶禁止"),
                    I18nText.of(
                            "불필요한 인사말 사용 금지",
                            "Prohibition of unnecessary greetings",
                            "不要な挨拶の使用禁止"
                    ),
                    HARD, FORBID
            ),
            new GuidelineRule(
                    "ANALYTICAL.OUTPUT.CITE_SOURCES",
                    I18nText.of("출처 명시", "Cite Sources", "出典の明示"),
                    I18nText.of(
                            "참조 데이터·이론·프레임워크의 출처를 가능한 한 명시",
                            "State sources of referenced data, theories, and frameworks whenever possible",
                            "参照データ・理論・フレームワークの出典を可能な限り明示"
                    ),
                    SOFT, REQUIRE
            ),
            new GuidelineRule(
                    "ANALYTICAL.OUTPUT.SUMMARY_FIRST",
                    I18nText.of("핵심 요약 선행", "Summary First", "核心要約先行"),
                    I18nText.of(
                            "결론/핵심 인사이트를 서두에 배치, 상세 분석은 이후 전개",
                            "Place conclusions/key insights at the beginning, followed by detailed analysis",
                            "結論/核心インサイトを冒頭に配置、詳細分析はその後に展開"
                    ),
                    HARD, REQUIRE
            ),
            new GuidelineRule(
                    "ANALYTICAL.OUTPUT.QUANT_QUAL_SEPARATION",
                    I18nText.of("정량/정성 구분", "Quantitative/Qualitative Separation", "定量/定性の区分"),
                    I18nText.of(
                            "정량적 데이터와 정성적 해석을 명확히 분리 표기",
                            "Clearly separate and label quantitative data and qualitative interpretation",
                            "定量的データと定性的解釈を明確に分離表記"
                    ),
                    SOFT, REQUIRE
            )
    );

    private AnalyticalGuidelines() {}

    @Override
    public List<GuidelineRule> principles() {
        return PRINCIPLES;
    }

    @Override
    public List<GuidelineRule> structuringRules() {
        return STRUCTURING_RULES;
    }

    @Override
    public List<GuidelineRule> qualityStandards() {
        return QUALITY_STANDARDS;
    }

    @Override
    public List<GuidelineRule> outputConstraints() {
        return OUTPUT_CONSTRAINTS;
    }
}


