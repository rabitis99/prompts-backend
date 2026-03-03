package org.example.sharedprompts.domain.prompt.common.guideline.content;

import org.example.sharedprompts.domain.prompt.common.guideline.policy.GuidelinePolicy;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.GuidelineRule;
import org.example.sharedprompts.domain.prompt.common.guideline.i18n.I18nText;
import java.util.List;

import static org.example.sharedprompts.domain.prompt.common.guideline.rule.RuleLevel.*;
import static org.example.sharedprompts.domain.prompt.common.guideline.rule.RuleType.*;

/**
 * TECHNICAL 도메인 가이드라인 — 정확성/실용성 중심 기술 작업
 */
public final class TechnicalGuidelines implements GuidelinePolicy {

    public static final TechnicalGuidelines INSTANCE = new TechnicalGuidelines();

    private static final List<GuidelineRule> PRINCIPLES = List.of(
            new GuidelineRule(
                    "TECHNICAL.PRINCIPLE.ACCURACY",
                    I18nText.of("정확성", "Accuracy", "正確性"),
                    I18nText.of(
                            "검증된 사실만 제공. 추측 금지",
                            "Only verified facts. No speculation",
                            "検証された事実のみ。推測禁止"
                    ),
                    HARD, REQUIRE
            ),
            new GuidelineRule(
                    "TECHNICAL.PRINCIPLE.CONTEXT",
                    I18nText.of("맥락", "Context", "文脈"),
                    I18nText.of(
                            "요청 범위 내에서만 응답. 불필요한 확장 금지",
                            "Respond within request scope only. No unnecessary extension",
                            "要求範囲内のみ応答。不要な拡張禁止"
                    ),
                    HARD, REQUIRE
            ),
            new GuidelineRule(
                    "TECHNICAL.PRINCIPLE.PRACTICALITY",
                    I18nText.of("실용성", "Practicality", "実用性"),
                    I18nText.of(
                            "실전 적용 가능한 내용 우선",
                            "Prioritize practical, applicable content",
                            "実践適用可能な内容を優先"
                    ),
                    SOFT, REQUIRE
            )
    );

    private static final List<GuidelineRule> STRUCTURING_RULES = List.of(
            new GuidelineRule(
                    "TECHNICAL.STRUCTURE.LOGICAL",
                    I18nText.of("논리적 구성", "Logical", "論理的"),
                    I18nText.of(
                            "제목, 하위 섹션으로 논리적 구성",
                            "Logical structure with headings and subsections",
                            "見出し、下位セクションで論理的構成"
                    ),
                    HARD, REQUIRE
            ),
            new GuidelineRule(
                    "TECHNICAL.STRUCTURE.READABILITY",
                    I18nText.of("가독성", "Readability", "可読性"),
                    I18nText.of(
                            "핵심→세부 순서로 정보 계층화",
                            "Hierarchy: core→details order",
                            "核心→詳細の順で情報階層化"
                    ),
                    SOFT, REQUIRE
            ),
            new GuidelineRule(
                    "TECHNICAL.STRUCTURE.NO_FLUFF",
                    I18nText.of("불필요 요소 제거", "No Fluff", "不要要素の除去"),
                    I18nText.of(
                            "인사말, 서론, 마무리 생략",
                            "Omit greetings, intro, closing",
                            "挨拶、前置き、結び省略"
                    ),
                    SOFT, REQUIRE
            ),
            new GuidelineRule(
                    "TECHNICAL.STRUCTURE.CONCISENESS",
                    I18nText.of("간결성", "Conciseness", "簡潔"),
                    I18nText.of(
                            "간결하고 직접적인 표현 사용",
                            "Use concise, direct expressions",
                            "簡潔で直接的な表現を使用"
                    ),
                    SOFT, REQUIRE
            )
    );

    private static final List<GuidelineRule> QUALITY_STANDARDS = List.of(
            new GuidelineRule(
                    "TECHNICAL.QUALITY.CLARITY",
                    I18nText.of("명확성", "Clarity", "明確性"),
                    I18nText.of(
                            "구체적 용어 사용",
                            "Use specific terms",
                            "具体的用語を使用"
                    ),
                    HARD, REQUIRE
            ),
            new GuidelineRule(
                    "TECHNICAL.QUALITY.ACTIONABILITY",
                    I18nText.of("실행 가능성", "Actionability", "実行可能性"),
                    I18nText.of(
                            "실행 가능한 지침 포함",
                            "Include actionable guidelines",
                            "実行可能な指針を含む"
                    ),
                    SOFT, REQUIRE
            ),
            new GuidelineRule(
                    "TECHNICAL.QUALITY.COMPLETENESS",
                    I18nText.of("완전성", "Completeness", "完全性"),
                    I18nText.of(
                            "완전한 응답 제공",
                            "Provide complete response",
                            "完全な応答を提供"
                    ),
                    SOFT, REQUIRE
            )
    );

    private static final List<GuidelineRule> OUTPUT_CONSTRAINTS = List.of(
            new GuidelineRule(
                    "TECHNICAL.OUTPUT.NO_GREETING",
                    I18nText.of("인사말 금지", "No Greetings", "挨拶禁止"),
                    I18nText.of(
                            "인사말 금지",
                            "No greetings",
                            "挨拶禁止"
                    ),
                    HARD, FORBID
            ),
            new GuidelineRule(
                    "TECHNICAL.OUTPUT.MINIMAL_INTRO",
                    I18nText.of("서론 최소화", "Minimal Intro", "前置き最小"),
                    I18nText.of(
                            "핵심으로 바로 진입",
                            "Get to core directly",
                            "核心に直接入る"
                    ),
                    SOFT, REQUIRE
            ),
            new GuidelineRule(
                    "TECHNICAL.OUTPUT.CONCISE_ENDING",
                    I18nText.of("마무리 간결", "Concise End", "簡潔な結び"),
                    I18nText.of(
                            "불필요한 마무리 지양",
                            "Avoid unnecessary closing",
                            "不要な結び避ける"
                    ),
                    SOFT, REQUIRE
            ),
            new GuidelineRule(
                    "TECHNICAL.OUTPUT.DIRECT_EXPRESSION",
                    I18nText.of("직접적 표현", "Direct", "直接的"),
                    I18nText.of(
                            "직접적 지시 사용",
                            "Use direct instructions",
                            "直接指示を使用"
                    ),
                    SOFT, REQUIRE
            )
    );

    private TechnicalGuidelines() {}

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

