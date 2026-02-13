package org.example.sharedprompts.domain.prompt.enums.guideline;

import java.util.List;

import static org.example.sharedprompts.domain.prompt.enums.guideline.RuleLevel.*;
import static org.example.sharedprompts.domain.prompt.enums.guideline.RuleType.*;

/**
 * GENERAL 도메인 가이드라인 — 보수적 안전 모드
 * <p>어떤 작업이든 최소한 합리적인 결과를 보장하는 "세이프 모드"</p>
 */
public final class GeneralGuidelines implements GuidelinePolicy {

    public static final GeneralGuidelines INSTANCE = new GeneralGuidelines();

    /** GENERAL 폴백 시 출력되는 한계 인정 메타-규칙 */
    public static final GuidelineRule LIMITATION_ACK = GuidelineRule.of(
            "GENERAL.META.LIMITATION_ACK",
            I18nText.of("도메인 비특화 안내", "Domain Non-Specialization Notice", "ドメイン非特化案内"),
            I18nText.of(
                    "이 응답은 범용 모드로 생성되었습니다. 특정 분야의 전문적 깊이가 필요한 경우, "
                            + "보다 적합한 카테고리와 작업 유형을 선택하면 더 나은 결과를 얻을 수 있습니다.",
                    "This response was generated in general-purpose mode. For domain-specific depth, "
                            + "selecting a more appropriate category and action type may yield better results.",
                    "この応答は汎用モードで生成されました。特定分野の専門的な深さが必要な場合、"
                            + "より適切なカテゴリとアクションタイプを選択するとより良い結果が得られます。"
            ),
            SOFT, ALLOW
    );

    private GeneralGuidelines() {}

    @Override
    public List<GuidelineRule> principles() {
        return List.of(
                GuidelineRule.of(
                        "GENERAL.PRINCIPLE.BALANCED_ACCURACY",
                        I18nText.of("균형 잡힌 정확성", "Balanced Accuracy", "バランスの取れた正確性"),
                        I18nText.of(
                                "검증된 정보를 우선하되, 맥락에 따라 합리적 추론과 유연한 표현도 허용",
                                "Prioritize verified information while allowing reasonable inference and flexible expression when contextually appropriate",
                                "検証された情報を優先しつつ、文脈に応じて合理的な推論と柔軟な表現も許容"
                        ),
                        SOFT, REQUIRE
                ),
                GuidelineRule.of(
                        "GENERAL.PRINCIPLE.ADAPTIVE_CONTEXT",
                        I18nText.of("적응적 맥락 파악", "Adaptive Context Recognition", "適応的文脈把握"),
                        I18nText.of(
                                "사용자 요청의 성격(정보형/감성형/실무형)을 파악하여 그에 맞는 접근 방식 선택",
                                "Recognize the nature of user requests (informational/emotional/practical) and choose an appropriate approach",
                                "利用者要求の性格（情報型/感性型/実務型）を把握し、適切なアプローチを選択"
                        ),
                        HARD, REQUIRE
                ),
                GuidelineRule.of(
                        "GENERAL.PRINCIPLE.UNIVERSAL_UTILITY",
                        I18nText.of("보편적 유용성", "Universal Utility", "普遍的有用性"),
                        I18nText.of(
                                "특정 도메인에 치우치지 않는 범용적이고 이해하기 쉬운 응답 제공",
                                "Provide universally useful and easily understandable responses without domain-specific bias",
                                "特定ドメインに偏らない汎用的で理解しやすい応答を提供"
                        ),
                        SOFT, REQUIRE
                )
        );
    }

    @Override
    public List<GuidelineRule> structuringRules() {
        return List.of(
                GuidelineRule.of(
                        "GENERAL.STRUCTURE.FLEXIBLE",
                        I18nText.of("유연한 구성", "Flexible Organization", "柔軟な構成"),
                        I18nText.of(
                                "내용에 적합한 구조를 자율적으로 선택 (강제 포맷 없음)",
                                "Autonomously choose a structure suitable for the content (no forced format)",
                                "内容に適した構造を自律的に選択（強制フォーマットなし）"
                        ),
                        SOFT, ALLOW
                ),
                GuidelineRule.of(
                        "GENERAL.STRUCTURE.READABILITY",
                        I18nText.of("가독성 확보", "Ensure Readability", "可読性の確保"),
                        I18nText.of(
                                "적절한 단락 구분과 형식 활용으로 읽기 쉬운 응답",
                                "Ensure readable responses with appropriate paragraph divisions and formatting",
                                "適切な段落区分と形式活用で読みやすい応答"
                        ),
                        SOFT, REQUIRE
                ),
                GuidelineRule.of(
                        "GENERAL.STRUCTURE.CORE_FIRST",
                        I18nText.of("핵심 우선", "Core First", "核心優先"),
                        I18nText.of(
                                "가장 중요한 정보를 먼저 제시하되, 부가 정보도 필요시 포함",
                                "Present the most important information first while including supplementary information when needed",
                                "最も重要な情報をまず提示しつつ、補足情報も必要時に含む"
                        ),
                        SOFT, REQUIRE
                ),
                GuidelineRule.of(
                        "GENERAL.STRUCTURE.APPROPRIATE_LENGTH",
                        I18nText.of("적절한 분량", "Appropriate Length", "適切な分量"),
                        I18nText.of(
                                "요청의 복잡도에 비례하는 분량. 과하지도 부족하지도 않게",
                                "Length proportional to the complexity of the request. Neither excessive nor insufficient",
                                "要求の複雑度に比例する分量。過度でも不足でもなく"
                        ),
                        SOFT, REQUIRE
                )
        );
    }

    @Override
    public List<GuidelineRule> qualityStandards() {
        return List.of(
                GuidelineRule.of(
                        "GENERAL.QUALITY.COMPREHENSIBILITY",
                        I18nText.of("이해 용이성", "Comprehensibility", "理解の容易さ"),
                        I18nText.of(
                                "전문 지식 없이도 이해할 수 있는 쉬운 표현 사용",
                                "Use easy expressions understandable without specialized knowledge",
                                "専門知識がなくても理解できる平易な表現を使用"
                        ),
                        SOFT, REQUIRE
                ),
                GuidelineRule.of(
                        "GENERAL.QUALITY.PRACTICALITY",
                        I18nText.of("실용성", "Practicality", "実用性"),
                        I18nText.of(
                                "가능한 한 실생활에 활용할 수 있는 구체적 정보 포함",
                                "Include specific information applicable to real life whenever possible",
                                "可能な限り実生活に活用できる具体的な情報を含む"
                        ),
                        SOFT, REQUIRE
                ),
                GuidelineRule.of(
                        "GENERAL.QUALITY.RELIABILITY",
                        I18nText.of("신뢰성", "Reliability", "信頼性"),
                        I18nText.of(
                                "불확실한 정보는 그 불확실성을 명시하고, 확인된 사실과 구분",
                                "Explicitly state uncertainty of uncertain information and distinguish from confirmed facts",
                                "不確実な情報はその不確実性を明示し、確認された事実と区分"
                        ),
                        HARD, REQUIRE
                )
        );
    }

    @Override
    public List<GuidelineRule> outputConstraints() {
        return List.of(
                GuidelineRule.of(
                        "GENERAL.OUTPUT.MINIMAL_GREETING",
                        I18nText.of("인사말 최소화", "Minimize Greetings", "挨拶の最小化"),
                        I18nText.of(
                                "짧은 인사는 허용하되 과도한 형식적 인사말 지양",
                                "Allow brief greetings but avoid excessive formal greetings",
                                "短い挨拶は許容するが、過度な形式的挨拶は避ける"
                        ),
                        SOFT, ALLOW
                ),
                GuidelineRule.of(
                        "GENERAL.OUTPUT.NATURAL_INTRO",
                        I18nText.of("자연스러운 도입", "Natural Introduction", "自然な導入"),
                        I18nText.of(
                                "맥락에 맞는 자연스러운 시작 (강제 서론 최소화 없음)",
                                "Natural opening suited to context (no forced introduction minimization)",
                                "文脈に合った自然な始まり（強制的な前置き最小化なし）"
                        ),
                        SOFT, ALLOW
                ),
                GuidelineRule.of(
                        "GENERAL.OUTPUT.APPROPRIATE_ENDING",
                        I18nText.of("적절한 마무리", "Appropriate Ending", "適切な結び"),
                        I18nText.of(
                                "요약이나 다음 단계 안내가 도움이 되면 포함",
                                "Include summary or next-step guidance if helpful",
                                "要約や次のステップの案内が有用であれば含む"
                        ),
                        SOFT, ALLOW
                )
        );
    }
}
