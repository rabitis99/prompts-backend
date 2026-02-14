package org.example.sharedprompts.domain.prompt.guideline;

import java.util.List;

import static org.example.sharedprompts.domain.prompt.guideline.RuleLevel.*;
import static org.example.sharedprompts.domain.prompt.guideline.RuleType.*;

/**
 * PRACTICAL 도메인 가이드라인 — 즉시 활용 가능한 실무
 */
public final class PracticalGuidelines implements GuidelinePolicy {

    public static final PracticalGuidelines INSTANCE = new PracticalGuidelines();

    private static final List<GuidelineRule> PRINCIPLES = List.of(
            new GuidelineRule(
                    "PRACTICAL.PRINCIPLE.IMMEDIATE_USE",
                    I18nText.of("즉시 활용 가능", "Immediately Actionable", "即座に活用可能"),
                    I18nText.of(
                            "바로 업무에 적용할 수 있는 실행 방안 제공. 복사하여 바로 사용 가능한 수준의 구체성",
                            "Provide action plans applicable to work immediately. Concrete enough to copy and use directly",
                            "すぐに業務に適用できる実行方針を提供。コピーしてそのまま使用可能なレベルの具体性"
                    ),
                    HARD, REQUIRE
            ),
            new GuidelineRule(
                    "PRACTICAL.PRINCIPLE.EFFICIENCY",
                    I18nText.of("효율성 지향", "Efficiency-Oriented", "効率性志向"),
                    I18nText.of(
                            "시간과 자원의 최적 활용 지향. 불필요한 절차나 과도한 부연 설명 지양",
                            "Optimize use of time and resources. Avoid unnecessary procedures or excessive supplementary explanations",
                            "時間と資源の最適な活用を指向。不要な手続きや過度な補足説明を避ける"
                    ),
                    SOFT, REQUIRE
            ),
            new GuidelineRule(
                    "PRACTICAL.PRINCIPLE.RESULT_ORIENTED",
                    I18nText.of("결과 중심", "Result-Oriented", "結果中心"),
                    I18nText.of(
                            "과정보다 성과와 결과물에 초점. 구체적 산출물·지표·달성 목표 명시",
                            "Focus on outcomes and deliverables over process. Specify concrete outputs, metrics, and goals",
                            "過程よりも成果と成果物に焦点。具体的な成果物・指標・達成目標を明示"
                    ),
                    HARD, REQUIRE
            )
    );

    private static final List<GuidelineRule> STRUCTURING_RULES = List.of(
            new GuidelineRule(
                    "PRACTICAL.STRUCTURE.CHECKLIST",
                    I18nText.of("체크리스트 형식", "Checklist Format", "チェックリスト形式"),
                    I18nText.of(
                            "실행 항목을 체크리스트나 단계별 목록으로 구조화",
                            "Structure action items as checklists or step-by-step lists",
                            "実行項目をチェックリストや段階的なリストで構造化"
                    ),
                    SOFT, REQUIRE
            ),
            new GuidelineRule(
                    "PRACTICAL.STRUCTURE.PRIORITY",
                    I18nText.of("우선순위 표기", "Priority Marking", "優先順位の表記"),
                    I18nText.of(
                            "작업의 중요도·긴급도를 명시하여 실행 순서 안내",
                            "Mark importance and urgency of tasks to guide execution order",
                            "作業の重要度・緊急度を明示して実行順序を案内"
                    ),
                    SOFT, REQUIRE
            ),
            new GuidelineRule(
                    "PRACTICAL.STRUCTURE.TEMPLATE",
                    I18nText.of("템플릿 제공", "Provide Templates", "テンプレートの提供"),
                    I18nText.of(
                            "문서·이메일·보고서는 바로 사용 가능한 템플릿 형태로 제시",
                            "Present documents, emails, and reports in ready-to-use template format",
                            "文書・メール・報告書はすぐに使用可能なテンプレート形式で提示"
                    ),
                    SOFT, REQUIRE
            ),
            new GuidelineRule(
                    "PRACTICAL.STRUCTURE.EXPECTED_EFFECT",
                    I18nText.of("기대 효과 명시", "State Expected Effects", "期待効果の明示"),
                    I18nText.of(
                            "각 행동 방안의 예상 결과나 기대 효과를 구체적으로 제시",
                            "Specifically present expected results or effects for each action plan",
                            "各行動方針の予想結果や期待効果を具体的に提示"
                    ),
                    SOFT, REQUIRE
            )
    );

    private static final List<GuidelineRule> QUALITY_STANDARDS = List.of(
            new GuidelineRule(
                    "PRACTICAL.QUALITY.FEASIBILITY",
                    I18nText.of("실행 가능성", "Feasibility", "実行可能性"),
                    I18nText.of(
                            "현실적 제약(예산·인력·시간) 고려한 실현 가능한 방안 제시",
                            "Present feasible plans considering realistic constraints (budget, personnel, time)",
                            "現実的な制約（予算・人員・時間）を考慮した実現可能な方策を提示"
                    ),
                    HARD, REQUIRE
            ),
            new GuidelineRule(
                    "PRACTICAL.QUALITY.CLARITY",
                    I18nText.of("명확성", "Clarity", "明確性"),
                    I18nText.of(
                            "실행 주체·시기·방법이 모호하지 않게 구체적 서술. \"적절히\" 같은 모호한 수식어 최소화",
                            "Describe execution subject, timing, and method concretely. Minimize vague modifiers like 'appropriately'",
                            "実行主体・時期・方法が曖昧にならないよう具体的に記述。「適切に」のような曖昧な修飾語を最小化"
                    ),
                    HARD, REQUIRE
            ),
            new GuidelineRule(
                    "PRACTICAL.QUALITY.SELF_CONTAINED",
                    I18nText.of("완결성", "Self-Contained", "完結性"),
                    I18nText.of(
                            "추가 확인 없이 실행에 옮길 수 있는 자급자족적 응답",
                            "Self-sufficient responses that can be executed without additional confirmation",
                            "追加確認なしで実行に移せる自給自足的な応答"
                    ),
                    SOFT, REQUIRE
            )
    );

    private static final List<GuidelineRule> OUTPUT_CONSTRAINTS = List.of(
            new GuidelineRule(
                    "PRACTICAL.OUTPUT.NO_GREETING",
                    I18nText.of("인사말 금지", "No Greetings", "挨拶禁止"),
                    I18nText.of(
                            "불필요한 인사말 사용 금지",
                            "Prohibition of unnecessary greetings",
                            "不要な挨拶の使用禁止"
                    ),
                    HARD, FORBID
            ),
            new GuidelineRule(
                    "PRACTICAL.OUTPUT.MINIMAL_INTRO",
                    I18nText.of("서론 최소화", "Minimize Introduction", "前置き最小化"),
                    I18nText.of(
                            "핵심 실행 사항으로 바로 진입",
                            "Get straight to the core action items",
                            "核心的な実行事項に直接入る"
                    ),
                    SOFT, REQUIRE
            ),
            new GuidelineRule(
                    "PRACTICAL.OUTPUT.NEXT_STEPS",
                    I18nText.of("마무리 → Next Steps", "Ending → Next Steps", "結び → Next Steps"),
                    I18nText.of(
                            "불필요한 마무리 대신 다음 단계 명시",
                            "State next steps instead of unnecessary closing phrases",
                            "不要な結びの代わりに次のステップを明示"
                    ),
                    SOFT, REQUIRE
            ),
            new GuidelineRule(
                    "PRACTICAL.OUTPUT.DIRECT_EXPRESSION",
                    I18nText.of("직접적 표현", "Direct Expression", "直接的表現"),
                    I18nText.of(
                            "간접적 표현보다 직접적 지시 사용",
                            "Use direct instructions rather than indirect expressions",
                            "間接的表現よりも直接的な指示を使用"
                    ),
                    SOFT, REQUIRE
            )
    );

    private PracticalGuidelines() {}

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


