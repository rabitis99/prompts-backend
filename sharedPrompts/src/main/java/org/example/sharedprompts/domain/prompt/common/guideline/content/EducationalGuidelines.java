package org.example.sharedprompts.domain.prompt.common.guideline.content;

import org.example.sharedprompts.domain.prompt.common.guideline.policy.GuidelinePolicy;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.GuidelineRule;
import org.example.sharedprompts.domain.prompt.common.guideline.i18n.I18nText;
import java.util.List;

import static org.example.sharedprompts.domain.prompt.common.guideline.rule.RuleLevel.*;
import static org.example.sharedprompts.domain.prompt.common.guideline.rule.RuleType.*;

/**
 * EDUCATIONAL 도메인 가이드라인 — 이해 촉진/단계적 학습
 */
public final class EducationalGuidelines implements GuidelinePolicy {

    public static final EducationalGuidelines INSTANCE = new EducationalGuidelines();

    private static final List<GuidelineRule> PRINCIPLES = List.of(
            new GuidelineRule(
                    "EDU.PRINCIPLE.UNDERSTANDING",
                    I18nText.of("이해 촉진", "Promote Understanding", "理解の促進"),
                    I18nText.of(
                            "새로운 개념을 학습자의 기존 지식과 연결. 유추·비유·실생활 예시 적극 활용",
                            "Connect new concepts to learners' existing knowledge. Actively use analogies, metaphors, and real-life examples",
                            "新しい概念を学習者の既存知識と接続。類推・比喩・実生活の例を積極的に活用"
                    ),
                    HARD, REQUIRE
            ),
            new GuidelineRule(
                    "EDU.PRINCIPLE.PROGRESSIVE",
                    I18nText.of("단계적 심화", "Progressive Deepening", "段階的深化"),
                    I18nText.of(
                            "기초→심화로 자연스럽게 이행하는 점진적 학습 경로. 각 단계에서 선행 지식 확인",
                            "Progressive learning path naturally transitioning from basics to advanced. Confirm prerequisite knowledge at each stage",
                            "基礎→深化へ自然に移行する漸進的な学習経路。各段階で前提知識を確認"
                    ),
                    HARD, REQUIRE
            ),
            new GuidelineRule(
                    "EDU.PRINCIPLE.ACTIVE_ENGAGEMENT",
                    I18nText.of("능동적 참여 유도", "Encourage Active Engagement", "能動的参加の誘導"),
                    I18nText.of(
                            "단순 정보 전달보다 학습자의 사고를 자극하는 질문·연습·자기 점검 요소 포함",
                            "Include questions, exercises, and self-check elements that stimulate learner thinking over simple information delivery",
                            "単純な情報伝達よりも学習者の思考を刺激する質問・練習・自己点検要素を含む"
                    ),
                    SOFT, REQUIRE
            )
    );

    private static final List<GuidelineRule> STRUCTURING_RULES = List.of(
            new GuidelineRule(
                    "EDU.STRUCTURE.CONCEPT_FIRST",
                    I18nText.of("개념 선행", "Concept First", "概念先行"),
                    I18nText.of(
                            "핵심 개념/용어를 먼저 정의한 후 상세 설명 진입",
                            "Define key concepts/terms first, then proceed to detailed explanations",
                            "核心概念/用語をまず定義した後、詳細説明に入る"
                    ),
                    HARD, REQUIRE
            ),
            new GuidelineRule(
                    "EDU.STRUCTURE.RICH_EXAMPLES",
                    I18nText.of("예시 풍부", "Rich Examples", "豊富な例示"),
                    I18nText.of(
                            "각 개념마다 구체적 예시·비유·실습 과제 병기",
                            "Accompany each concept with concrete examples, analogies, and practice exercises",
                            "各概念ごとに具体的な例示・比喩・実習課題を併記"
                    ),
                    SOFT, REQUIRE
            ),
            new GuidelineRule(
                    "EDU.STRUCTURE.KEY_TAKEAWAYS",
                    I18nText.of("요약 및 복습", "Summary and Review", "要約と復習"),
                    I18nText.of(
                            "주요 섹션 끝에 Key Takeaways 배치",
                            "Place Key Takeaways at the end of major sections",
                            "主要セクションの最後にKey Takeawaysを配置"
                    ),
                    SOFT, REQUIRE
            ),
            new GuidelineRule(
                    "EDU.STRUCTURE.DIFFICULTY_LEVEL",
                    I18nText.of("난이도 표기", "Difficulty Marking", "難易度の表記"),
                    I18nText.of(
                            "내용의 난이도(기초/중급/심화) 명시",
                            "Mark content difficulty level (basic/intermediate/advanced)",
                            "内容の難易度（基礎/中級/深化）を明示"
                    ),
                    SOFT, REQUIRE
            )
    );

    private static final List<GuidelineRule> QUALITY_STANDARDS = List.of(
            new GuidelineRule(
                    "EDU.QUALITY.ACCURACY",
                    I18nText.of("정확성", "Accuracy", "正確性"),
                    I18nText.of(
                            "교육 내용의 사실적 정확성 보장. 오개념으로 학습자 오도 금지",
                            "Ensure factual accuracy of educational content. Never mislead learners with misconceptions",
                            "教育内容の事実的正確性を保証。誤った概念で学習者を誤導することを禁止"
                    ),
                    HARD, REQUIRE
            ),
            new GuidelineRule(
                    "EDU.QUALITY.ACCESSIBILITY",
                    I18nText.of("접근성", "Accessibility", "アクセシビリティ"),
                    I18nText.of(
                            "학습자 수준에 맞는 언어와 설명 깊이. 전문 용어는 반드시 풀어서 설명",
                            "Use language and explanation depth appropriate to learner level. Always explain technical terms in plain language",
                            "学習者のレベルに合った言語と説明の深さ。専門用語は必ず分かりやすく説明"
                    ),
                    HARD, REQUIRE
            ),
            new GuidelineRule(
                    "EDU.QUALITY.MOTIVATION",
                    I18nText.of("동기 부여", "Motivation", "動機付け"),
                    I18nText.of(
                            "학습의 실용적 가치와 활용 방안을 제시하여 \"왜 배우는가\" 동기 부여",
                            "Motivate by presenting practical value and use cases of learning — 'why learn this'",
                            "学習の実用的価値と活用方法を提示して「なぜ学ぶのか」動機付け"
                    ),
                    SOFT, REQUIRE
            )
    );

    private static final List<GuidelineRule> OUTPUT_CONSTRAINTS = List.of(
            new GuidelineRule(
                    "EDU.OUTPUT.FRIENDLY_INTRO",
                    I18nText.of("친근한 도입 허용", "Friendly Introduction Allowed", "親しみやすい導入の許容"),
                    I18nText.of(
                            "학습 동기를 유발하는 간결한 도입부 허용 (과도한 인사말은 지양)",
                            "Allow concise introductions that motivate learning (avoid excessive greetings)",
                            "学習動機を誘発する簡潔な導入部を許容（過度な挨拶は避ける）"
                    ),
                    SOFT, ALLOW
            ),
            new GuidelineRule(
                    "EDU.OUTPUT.ENCOURAGING_ENDING",
                    I18nText.of("격려의 마무리", "Encouraging Ending", "励ましの結び"),
                    I18nText.of(
                            "학습자를 격려하는 간결한 마무리 문구 허용",
                            "Allow concise closing phrases that encourage the learner",
                            "学習者を励ます簡潔な結びの文句を許容"
                    ),
                    SOFT, ALLOW
            ),
            new GuidelineRule(
                    "EDU.OUTPUT.LEARNING_ACTIONS",
                    I18nText.of("학습 행동 유도", "Encourage Learning Actions", "学習行動の誘導"),
                    I18nText.of(
                            "\"~해보세요\", \"~를 확인하세요\" 등 직접적 학습 지시 사용",
                            "Use direct learning instructions such as 'Try ~', 'Check ~'",
                            "「～してみてください」「～を確認してください」など直接的な学習指示を使用"
                    ),
                    SOFT, REQUIRE
            )
    );

    private EducationalGuidelines() {}

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


