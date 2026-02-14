package org.example.sharedprompts.domain.prompt.guideline;

import java.util.List;

import static org.example.sharedprompts.domain.prompt.guideline.RuleLevel.*;
import static org.example.sharedprompts.domain.prompt.guideline.RuleType.*;

/**
 * CREATIVE 도메인 가이드라인 — 독창성/감성 중심 창작 작업
 */
public final class CreativeGuidelines implements GuidelinePolicy {

    public static final CreativeGuidelines INSTANCE = new CreativeGuidelines();

    private static final List<GuidelineRule> PRINCIPLES = List.of(
            // 핵심 원칙
            new GuidelineRule(
                    "CREATIVE.PRINCIPLE.ORIGINALITY",
                    I18nText.of("독창성 추구", "Pursue Originality", "独創性の追求"),
                    I18nText.of(
                            "기존 틀에 얽매이지 않는 새로운 관점과 표현을 적극 탐색. 창의적 도약과 예상 밖의 연결 장려",
                            "Actively explore new perspectives and expressions unconstrained by existing frameworks. Encourage creative leaps and unexpected connections",
                            "既存の枠にとらわれない新しい視点と表現を積極的に探索。創造的な飛躍と予想外のつながりを奨励"
                    ),
                    HARD, REQUIRE
            ),
            new GuidelineRule(
                    "CREATIVE.PRINCIPLE.EMOTIONAL_RESONANCE",
                    I18nText.of("감성적 공명", "Emotional Resonance", "感性的共鳴"),
                    I18nText.of(
                            "독자/사용자의 감정과 공감을 불러일으키는 표현 지향. 논리적 설명보다 감각적 체험 우선",
                            "Orient towards expressions that evoke emotions and empathy. Prioritize sensory experience over logical explanation",
                            "読者/利用者の感情と共感を呼び起こす表現を指向。論理的説明よりも感覚的体験を優先"
                    ),
                    HARD, REQUIRE
            ),
            new GuidelineRule(
                    "CREATIVE.PRINCIPLE.CONTEXTUAL_FREEDOM",
                    I18nText.of("맥락적 자유", "Contextual Freedom", "文脈的自由"),
                    I18nText.of(
                            "작품 의도와 분위기에 맞는 표현이라면 모호함·은유·비유 적극 허용. 주제와 분위기에 자연스럽게 부합하는 흐름 추구",
                            "Actively allow ambiguity, metaphor, and analogy if they suit the work's intent and mood. Pursue a flow that naturally aligns with theme and atmosphere",
                            "作品の意図と雰囲気に合う表現であれば曖昧さ・隠喩・比喩を積極的に許容。テーマと雰囲気に自然に合致する流れを追求"
                    ),
                    SOFT, ALLOW
            ),
            // 의도적 불완전성 허용 (CREATIVE 전용 반-규칙)
            new GuidelineRule(
                    "CREATIVE.ANTI.NO_FORCED_COMPLETION",
                    I18nText.of("완전성 강제 금지", "No Forced Completion", "完全性強制禁止"),
                    I18nText.of(
                            "완전한 결론을 강제하지 않음. 감정 전달이 충분하면 불완전한 결론도 허용",
                            "Do not force complete conclusions. Incomplete conclusions are allowed if emotional delivery is sufficient",
                            "完全な結論を強制しない。感情伝達が十分であれば不完全な結論も許容"
                    ),
                    HARD, FORBID
            ),
            new GuidelineRule(
                    "CREATIVE.ANTI.NO_FORCED_CONCLUSION",
                    I18nText.of("결론 강제 금지", "No Forced Conclusion", "結論強制禁止"),
                    I18nText.of(
                            "명확한 결론을 강제하지 않음. 질문으로 끝나는 응답이나 열린 결말 허용",
                            "Do not force clear conclusions. Responses ending with questions or open endings are allowed",
                            "明確な結論を強制しない。質問で終わる応答や開かれた結末を許容"
                    ),
                    HARD, FORBID
            ),
            new GuidelineRule(
                    "CREATIVE.ANTI.NO_FORCED_MESSAGE",
                    I18nText.of("메시지 강제 금지", "No Forced Message", "メッセージ強制禁止"),
                    I18nText.of(
                            "단일 메시지를 강제하지 않음. 의미가 열려 있는 표현이나 다중 해석 가능한 표현 허용",
                            "Do not force a single message. Open-meaning expressions and multi-interpretable phrases are allowed",
                            "単一のメッセージを強制しない。意味が開かれた表現や多重解釈可能な表現を許容"
                    ),
                    HARD, FORBID
            ),
            new GuidelineRule(
                    "CREATIVE.ANTI.NO_FORCED_STRUCTURE",
                    I18nText.of("구조 강제 금지", "No Forced Structure", "構造強制禁止"),
                    I18nText.of(
                            "목록·표·단계별 구분 등 기술적 구조화를 강요하지 않음",
                            "Do not force technical structuring such as lists, tables, or step-by-step divisions",
                            "リスト・表・段階的区分などの技術的構造化を強要しない"
                    ),
                    HARD, FORBID
            )
    );

    private static final List<GuidelineRule> STRUCTURING_RULES = List.of(
            new GuidelineRule(
                    "CREATIVE.STRUCTURE.NATURAL_FLOW",
                    I18nText.of("자연스러운 흐름", "Natural Flow", "自然な流れ"),
                    I18nText.of(
                            "기계적 단계 구분보다 내용의 자연스러운 전개와 호흡 우선",
                            "Prioritize natural development and rhythm over mechanical step-by-step divisions",
                            "機械的な段階区分よりも内容の自然な展開とリズムを優先"
                    ),
                    HARD, REQUIRE
            ),
            new GuidelineRule(
                    "CREATIVE.STRUCTURE.SENSORY_EXPRESSION",
                    I18nText.of("감각적 표현 장려", "Encourage Sensory Expression", "感覚的表現の奨励"),
                    I18nText.of(
                            "추상적 개념도 구체적 이미지·비유·감각적 묘사로 전달",
                            "Convey abstract concepts through concrete images, analogies, and sensory descriptions",
                            "抽象的な概念も具体的なイメージ・比喩・感覚的描写で伝達"
                    ),
                    SOFT, REQUIRE
            ),
            new GuidelineRule(
                    "CREATIVE.STRUCTURE.MOOD_CONSISTENCY",
                    I18nText.of("분위기 일관성", "Mood Consistency", "雰囲気の一貫性"),
                    I18nText.of(
                            "작품 전체의 톤·분위기·정서적 색채의 통일감 유지",
                            "Maintain unified tone, mood, and emotional coloring throughout the work",
                            "作品全体のトーン・雰囲気・情緒的な色彩の統一感を維持"
                    ),
                    HARD, REQUIRE
            ),
            new GuidelineRule(
                    "CREATIVE.STRUCTURE.WHITE_SPACE",
                    I18nText.of("여백의 활용", "Use of White Space", "余白の活用"),
                    I18nText.of(
                            "모든 것을 설명하지 않고, 독자의 상상력이 채울 공간을 남김",
                            "Leave space for the reader's imagination rather than explaining everything",
                            "すべてを説明せず、読者の想像力が埋める余白を残す"
                    ),
                    SOFT, ALLOW
            )
    );

    private static final List<GuidelineRule> QUALITY_STANDARDS = List.of(
            new GuidelineRule(
                    "CREATIVE.QUALITY.IMMERSION",
                    I18nText.of("몰입감", "Immersion", "没入感"),
                    I18nText.of(
                            "독자가 작품 세계에 자연스럽게 빠져들 수 있는 생생한 장면과 감정 묘사",
                            "Vivid scenes and emotional descriptions that naturally draw the reader into the work's world",
                            "読者が作品世界に自然に引き込まれる生き生きとした場面と感情描写"
                    ),
                    HARD, REQUIRE
            ),
            new GuidelineRule(
                    "CREATIVE.QUALITY.ORIGINALITY",
                    I18nText.of("독창성", "Originality", "独創性"),
                    I18nText.of(
                            "진부한 표현·클리셰 회피. 새로운 시각, 예상 밖의 전개, 신선한 비유 활용",
                            "Avoid clichés and trite expressions. Use fresh perspectives, unexpected developments, and novel analogies",
                            "陳腐な表現・クリシェの回避。新しい視角、予想外の展開、新鮮な比喩を活用"
                    ),
                    HARD, REQUIRE
            ),
            new GuidelineRule(
                    "CREATIVE.QUALITY.EMOTIONAL_DEPTH",
                    I18nText.of("정서적 울림", "Emotional Depth", "情緒的な響き"),
                    I18nText.of(
                            "단순한 정보 전달을 넘어 감동·여운·성찰을 불러일으키는 깊이",
                            "Depth that evokes emotion, lingering resonance, and reflection beyond simple information delivery",
                            "単純な情報伝達を超えて感動・余韻・省察を呼び起こす深さ"
                    ),
                    SOFT, REQUIRE
            )
    );

    private static final List<GuidelineRule> OUTPUT_CONSTRAINTS = List.of(
            new GuidelineRule(
                    "CREATIVE.OUTPUT.FORMAT_FREEDOM",
                    I18nText.of("형식 자유", "Format Freedom", "形式の自由"),
                    I18nText.of(
                            "창작물의 장르와 의도에 따라 자유로운 형식 허용. 강제 템플릿 없음",
                            "Allow free formatting according to the genre and intent of the creative work. No forced templates",
                            "創作物のジャンルと意図に応じた自由な形式を許容。強制テンプレートなし"
                    ),
                    HARD, ALLOW
            ),
            new GuidelineRule(
                    "CREATIVE.OUTPUT.FREE_INTRO",
                    I18nText.of("도입부 자유", "Free Introduction", "導入部の自由"),
                    I18nText.of(
                            "서정적·묘사적·대화적 시작 모두 허용 (작품 분위기에 맞다면)",
                            "Allow lyrical, descriptive, or dialogue-based openings (if they suit the work's mood)",
                            "叙情的・描写的・対話的な始まりをすべて許容（作品の雰囲気に合えば）"
                    ),
                    SOFT, ALLOW
            ),
            new GuidelineRule(
                    "CREATIVE.OUTPUT.LINGERING_ENDING",
                    I18nText.of("마무리 여운", "Lingering Ending", "余韻のある結び"),
                    I18nText.of(
                            "깔끔한 종결보다 독자에게 여운과 생각거리를 남기는 열린 마무리 권장",
                            "Recommend open endings that leave lingering thoughts rather than clean conclusions",
                            "きれいな終結よりも読者に余韻と考える材料を残す開かれた結びを推奨"
                    ),
                    SOFT, ALLOW
            )
    );

    private CreativeGuidelines() {}

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


