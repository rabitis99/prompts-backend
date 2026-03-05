package org.example.sharedprompts.domain.prompt.common.enums;

import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.i18n.I18nKey;
import org.example.sharedprompts.domain.prompt.common.i18n.I18nRegistry;
import org.example.sharedprompts.domain.prompt.common.i18n.I18nText;

import java.util.List;

@Getter
public enum StyleType implements StableKeyedEnum {

    NARRATIVE(
            "STYLE.NARRATIVE",
            StyleAxis.STRUCTURE,
            I18nKey.of("style.narrative.display_name"),
            I18nKey.of("style.narrative.guideline"),
            false,
            true,
            true
    ),

    BULLET(
            "STYLE.BULLET",
            StyleAxis.FORMAT,
            I18nKey.of("style.bullet.display_name"),
            I18nKey.of("style.bullet.guideline"),
            true,
            false,
            false
    ),

    CONCISE(
            "STYLE.CONCISE",
            StyleAxis.FORMAT,
            I18nKey.of("style.concise.display_name"),
            I18nKey.of("style.concise.guideline"),
            true,
            false,
            false
    ),

    FORMATTED(
            "STYLE.FORMATTED",
            StyleAxis.FORMAT,
            I18nKey.of("style.formatted.display_name"),
            I18nKey.of("style.formatted.guideline"),
            true,
            false,
            false
    ),

    DESCRIPTIVE(
            "STYLE.DESCRIPTIVE",
            StyleAxis.DEPTH,
            I18nKey.of("style.descriptive.display_name"),
            I18nKey.of("style.descriptive.guideline"),
            false,
            true,
            true
    ),

    INSTRUCTIVE(
            "STYLE.INSTRUCTIVE",
            StyleAxis.FUNCTION,
            I18nKey.of("style.instructive.display_name"),
            I18nKey.of("style.instructive.guideline"),
            false,
            false,
            false
    ),

    QUESTION_ANSWER(
            "STYLE.QUESTION_ANSWER",
            StyleAxis.FUNCTION,
            I18nKey.of("style.question_answer.display_name"),
            I18nKey.of("style.question_answer.guideline"),
            false,
            false,
            false
    ),

    STORYTELLING(
            "STYLE.STORYTELLING",
            StyleAxis.STRUCTURE,
            I18nKey.of("style.storytelling.display_name"),
            I18nKey.of("style.storytelling.guideline"),
            false,
            true,
            true
    ),

    DIALOGUE(
            "STYLE.DIALOGUE",
            StyleAxis.STRUCTURE,
            I18nKey.of("style.dialogue.display_name"),
            I18nKey.of("style.dialogue.guideline"),
            false,
            true,
            true
    ),

    COMPARATIVE(
            "STYLE.COMPARATIVE",
            StyleAxis.DEPTH,
            I18nKey.of("style.comparative.display_name"),
            I18nKey.of("style.comparative.guideline"),
            false,
            true,
            false
    ),

    ANALYTICAL(
            "STYLE.ANALYTICAL",
            StyleAxis.DEPTH,
            I18nKey.of("style.analytical.display_name"),
            I18nKey.of("style.analytical.guideline"),
            false,
            true,
            false
    ),

    CREATIVE(
            "STYLE.CREATIVE",
            StyleAxis.FUNCTION,
            I18nKey.of("style.creative.display_name"),
            I18nKey.of("style.creative.guideline"),
            false,
            true,
            true
    ),

    TECHNICAL(
            "STYLE.TECHNICAL",
            StyleAxis.FUNCTION,
            I18nKey.of("style.technical.display_name"),
            I18nKey.of("style.technical.guideline"),
            false,
            false,
            false
    ),

    DETAILED(
            "STYLE.DETAILED",
            StyleAxis.DEPTH,
            I18nKey.of("style.detailed.display_name"),
            I18nKey.of("style.detailed.guideline"),
            false,
            true,
            false
    );

    static {
        I18nRegistry registry = I18nRegistry.global();

        // Display names: 현재는 한국어만 정의되어 있으므로, en/ja는 일단 동일한 값을 사용한다.
        registry.register(I18nKey.of("style.narrative.display_name"),
                I18nText.of("서사형", "서사형", "서사형"));
        registry.register(I18nKey.of("style.bullet.display_name"),
                I18nText.of("글머리형", "글머리형", "글머리형"));
        registry.register(I18nKey.of("style.concise.display_name"),
                I18nText.of("간결형", "간결형", "간결형"));
        registry.register(I18nKey.of("style.formatted.display_name"),
                I18nText.of("서식형", "서식형", "서식형"));
        registry.register(I18nKey.of("style.descriptive.display_name"),
                I18nText.of("묘사형", "묘사형", "묘사형"));
        registry.register(I18nKey.of("style.instructive.display_name"),
                I18nText.of("설명/가이드형", "설명/가이드형", "설명/ガイド"));
        registry.register(I18nKey.of("style.question_answer.display_name"),
                I18nText.of("질문-답변형", "질문-답변형", "質問-回答型"));
        registry.register(I18nKey.of("style.storytelling.display_name"),
                I18nText.of("스토리텔링형", "스토리텔링형", "ストーリーテリング"));
        registry.register(I18nKey.of("style.dialogue.display_name"),
                I18nText.of("대화형", "대화형", "対話型"));
        registry.register(I18nKey.of("style.comparative.display_name"),
                I18nText.of("비교형", "비교형", "比較型"));
        registry.register(I18nKey.of("style.analytical.display_name"),
                I18nText.of("분석형", "분석형", "分析型"));
        registry.register(I18nKey.of("style.creative.display_name"),
                I18nText.of("창의형", "창의형", "創造型"));
        registry.register(I18nKey.of("style.technical.display_name"),
                I18nText.of("기술형", "기술형", "技術型"));
        registry.register(I18nKey.of("style.detailed.display_name"),
                I18nText.of("상세형", "상세형", "詳細型"));

        // Guidelines: 기존 ko/en/ja 문자열을 그대로 중앙 레지스트리에 등록한다.
        registry.register(I18nKey.of("style.narrative.guideline"),
                I18nText.of("이야기 형식, 사건/경험 중심 서술",
                        "Narrative format, focus on events/experiences",
                        "物語形式、出来事・体験中心の叙述"));
        registry.register(I18nKey.of("style.bullet.guideline"),
                I18nText.of("핵심 포인트, 간결한 글머리 목록",
                        "Key points, concise bullet list",
                        "要点、簡潔な箇条書き"));
        registry.register(I18nKey.of("style.concise.guideline"),
                I18nText.of("핵심 정보만, 불필요한 설명 제거",
                        "Essential information only, remove unnecessary details",
                        "要点のみ、不要な説明を省く"));
        registry.register(I18nKey.of("style.formatted.guideline"),
                I18nText.of("명확한 구조, 정해진 서식 준수",
                        "Clear structure, follow predefined format",
                        "明確な構造、定められたフォーマットに従う"));
        registry.register(I18nKey.of("style.descriptive.guideline"),
                I18nText.of("감각적/시각적 요소 강조, 상세한 묘사",
                        "Emphasize sensory/visual details, vivid descriptions",
                        "感覚的・視覚的要素を強調、詳細な描写"));
        registry.register(I18nKey.of("style.instructive.guideline"),
            I18nText.of("단계별 명확한 안내, 절차 설명",
                    "Step-by-step clear guidance, procedure explanation",
                    "段階的な明確な案内、手順説明"));
        registry.register(I18nKey.of("style.question_answer.guideline"),
                I18nText.of("질문-답변 형식, 구조화된 정보 전달",
                        "Question-answer format, structured information delivery",
                        "質問-回答形式、構造化された情報提供"));
        registry.register(I18nKey.of("style.storytelling.guideline"),
                I18nText.of("스토리텔링 기법, 감정 유발",
                        "Storytelling techniques, evoke emotions",
                        "ストーリーテリング技法、感情に訴える"));
        registry.register(I18nKey.of("style.dialogue.guideline"),
                I18nText.of("대화 형식, 다자간 대화 전개",
                        "Dialogue format, multi-party conversation",
                        "会話形式、複数者間の対話展開"));
        registry.register(I18nKey.of("style.comparative.guideline"),
                I18nText.of("대상 비교, 차이점/공통점 강조",
                        "Compare subjects, highlight differences/similarities",
                        "対象比較、違い・共通点を強調"));
        registry.register(I18nKey.of("style.analytical.guideline"),
                I18nText.of("다양한 관점, 깊이 있는 분석",
                        "Multiple perspectives, in-depth analysis",
                        "複数の視点、深い分析"));
        registry.register(I18nKey.of("style.creative.guideline"),
                I18nText.of("독창적 아이디어, 새로운 접근 방식",
                        "Original ideas, innovative approaches",
                        "独創的なアイデア、新しいアプローチ"));
        registry.register(I18nKey.of("style.technical.guideline"),
                I18nText.of("정확한 기술 용어, 상세한 기술 설명",
                        "Precise technical terms, detailed technical explanations",
                        "正確な技術用語、詳細な技術説明"));
        registry.register(I18nKey.of("style.detailed.guideline"),
                I18nText.of("상세하고 구체적 설명, 깊이 있는 이해",
                        "Thorough and specific explanations, deep understanding",
                        "詳細かつ具体的な説明、深い理解"));
    }

    /** Stable serialization-safe identifier */
    private final String key;

    /** 스타일이 주로 다루는 축(구조/깊이/형식/기능) */
    private final StyleAxis axis;

    /** UI 표시용 (다국어 키) */
    private final I18nKey displayNameKey;

    /** Style guideline per language (다국어 키) */
    private final I18nKey guidelineKey;

    /** 리스트/글머리표 기반 스타일 여부 */
    private final boolean listFriendly;
    /** 장문/서사형에 적합한지 여부 */
    private final boolean longFormFriendly;
    /** 환각/픽션 성향이 강해 사실형 작업에 주의가 필요한지 여부 */
    private final boolean highHallucinationRisk;

    StyleType(
            String key,
            StyleAxis axis,
            I18nKey displayNameKey,
            I18nKey guidelineKey,
            boolean listFriendly,
            boolean longFormFriendly,
            boolean highHallucinationRisk
    ) {
        this.key = key;
        this.axis = axis;
        this.displayNameKey = displayNameKey;
        this.guidelineKey = guidelineKey;
        this.listFriendly = listFriendly;
        this.longFormFriendly = longFormFriendly;
        this.highHallucinationRisk = highHallucinationRisk;
    }

    @Override
    public String key() {
        return key;
    }

    /**
     * UI 기본 표시는 기존과 동일하게 한국어를 사용한다.
     */
    public String getDisplayName() {
        return I18nRegistry.global().lookup(displayNameKey, LanguageType.KOREAN);
    }

    /**
     * 언어 타입에 따라 적절한 스타일 가이드라인을 반환한다.
     */
    public String getGuidelineByLang(LanguageType lang) {
        return I18nRegistry.global().lookup(guidelineKey, lang);
    }

    /**
     * 영어 기준 스타일 가이드라인을 반환한다.
     * (기존 호출부의 getGuidelineEn()을 대체하기 위한 헬퍼)
     */
    public String getGuidelineEn() {
        return getGuidelineByLang(LanguageType.ENGLISH);
    }

    /**
     * 엔진 레벨에서 사용하는 정규화된 스타일 시그널을 반환한다.
     *
     * <p>여러 값이 구조적으로 유사한 경우, 다음과 같이 대표 스타일로 매핑된다.</p>
     *
+     * <ul>
     *   <li>STORYTELLING → NARRATIVE</li>
     *   <li>ANALYTICAL, COMPARATIVE → DESCRIPTIVE</li>
     *   <li>FORMATTED → CONCISE (구조적 측면에서 동일한 “정리형” 스타일)</li>
     *   <li>그 외 → 자기 자신</li>
     * </ul>
     */
    public StyleType getCanonicalStyle() {
        return switch (this) {
            case STORYTELLING -> NARRATIVE;
            case ANALYTICAL, COMPARATIVE -> DESCRIPTIVE;
            case FORMATTED -> CONCISE;
            default -> this;
        };
    }

    /**
     * 이 StyleType이 추천되는 TaskDomain 목록을 반환한다.
     * <p>여러 도메인에 적합한 스타일은 여러 도메인을 반환할 수 있다.</p>
     *
     * @return 추천되는 TaskDomain 목록 (비어있지 않음)
     */
    public List<TaskDomain> getRecommendedDomains() {
        return switch (this) {
            // CREATIVE 도메인 추천 스타일
            case NARRATIVE, STORYTELLING, DESCRIPTIVE, CREATIVE, DIALOGUE ->
                    List.of(TaskDomain.CREATIVE);

            // TECHNICAL 도메인 추천 스타일
            case TECHNICAL ->
                    List.of(TaskDomain.TECHNICAL);

            // ANALYTICAL 도메인 추천 스타일
            case ANALYTICAL, COMPARATIVE ->
                    List.of(TaskDomain.ANALYTICAL);

            // PRACTICAL 도메인 추천 스타일
            case BULLET ->
                    List.of(TaskDomain.PRACTICAL);

            // EDUCATIONAL 도메인 추천 스타일
            case DETAILED ->
                    List.of(TaskDomain.EDUCATIONAL);

            // 여러 도메인에 적합한 범용 스타일
            case INSTRUCTIVE ->
                    List.of(TaskDomain.TECHNICAL, TaskDomain.PRACTICAL, TaskDomain.EDUCATIONAL);

            case QUESTION_ANSWER ->
                    List.of(TaskDomain.ANALYTICAL, TaskDomain.EDUCATIONAL, TaskDomain.GENERAL);

            // TECHNICAL + PRACTICAL 도메인에 적합한 스타일
            case FORMATTED, CONCISE ->
                    List.of(TaskDomain.TECHNICAL, TaskDomain.PRACTICAL);
        };
    }
}

