package org.example.sharedprompts.domain.prompt.common.enums;

import lombok.Getter;

import java.util.List;

import static org.example.sharedprompts.domain.prompt.common.enums.I18nUtils.getByLang;

@Getter
public enum StyleType {

    NARRATIVE(
            StyleAxis.STRUCTURE,
            "서사형",
            "이야기 형식, 사건/경험 중심 서술",
            "Narrative format, focus on events/experiences",
            "物語形式、出来事・体験中心の叙述",
            false,
            true,
            true
    ),

    BULLET(
            StyleAxis.FORMAT,
            "글머리형",
            "핵심 포인트, 간결한 글머리 목록",
            "Key points, concise bullet list",
            "要点、簡潔な箇条書き",
            true,
            false,
            false
    ),

    CONCISE(
            StyleAxis.FORMAT,
            "간결형",
            "핵심 정보만, 불필요한 설명 제거",
            "Essential information only, remove unnecessary details",
            "要点のみ、不要な説明を省く",
            true,
            false,
            false
    ),

    FORMATTED(
            StyleAxis.FORMAT,
            "서식형",
            "명확한 구조, 정해진 서식 준수",
            "Clear structure, follow predefined format",
            "明確な構造、定められたフォーマットに従う",
            true,
            false,
            false
    ),

    DESCRIPTIVE(
            StyleAxis.DEPTH,
            "묘사형",
            "감각적/시각적 요소 강조, 상세한 묘사",
            "Emphasize sensory/visual details, vivid descriptions",
            "感覚的・視覚的要素を強調、詳細な描写",
            false,
            true,
            true
    ),

    INSTRUCTIVE(
            StyleAxis.FUNCTION,
            "설명/가이드형",
            "단계별 명확한 안내, 절차 설명",
            "Step-by-step clear guidance, procedure explanation",
            "段階的な明確な案内、手順説明",
            false,
            false,
            false
    ),

    QUESTION_ANSWER(
            StyleAxis.FUNCTION,
            "질문-답변형",
            "질문-답변 형식, 구조화된 정보 전달",
            "Question-answer format, structured information delivery",
            "質問-回答形式、構造化された情報提供",
            false,
            false,
            false
    ),

    STORYTELLING(
            StyleAxis.STRUCTURE,
            "스토리텔링형",
            "스토리텔링 기법, 감정 유발",
            "Storytelling techniques, evoke emotions",
            "ストーリーテリング技法、感情に訴える",
            false,
            true,
            true
    ),

    DIALOGUE(
            StyleAxis.STRUCTURE,
            "대화형",
            "대화 형식, 다자간 대화 전개",
            "Dialogue format, multi-party conversation",
            "会話形式、複数者間の対話展開",
            false,
            true,
            true
    ),

    COMPARATIVE(
            StyleAxis.DEPTH,
            "비교형",
            "대상 비교, 차이점/공통점 강조",
            "Compare subjects, highlight differences/similarities",
            "対象比較、違い・共通点を強調",
            false,
            true,
            false
    ),

    ANALYTICAL(
            StyleAxis.DEPTH,
            "분석형",
            "다양한 관점, 깊이 있는 분석",
            "Multiple perspectives, in-depth analysis",
            "複数の視点、深い分析",
            false,
            true,
            false
    ),

    CREATIVE(
            StyleAxis.FUNCTION,
            "창의형",
            "독창적 아이디어, 새로운 접근 방식",
            "Original ideas, innovative approaches",
            "独創的なアイデア、新しいアプローチ",
            false,
            true,
            true
    ),

    TECHNICAL(
            StyleAxis.FUNCTION,
            "기술형",
            "정확한 기술 용어, 상세한 기술 설명",
            "Precise technical terms, detailed technical explanations",
            "正確な技術用語、詳細な技術説明",
            false,
            false,
            false
    ),

    DETAILED(
            StyleAxis.DEPTH,
            "상세형",
            "상세하고 구체적 설명, 깊이 있는 이해",
            "Thorough and specific explanations, deep understanding",
            "詳細かつ具体的な説明、深い理解",
            false,
            true,
            false
    );

    /** 스타일이 주로 다루는 축(구조/깊이/형식/기능) */
    private final StyleAxis axis;

    /** UI 표시용 */
    private final String displayName;

    /** Style guideline per language */
    private final String guidelineKo;
    private final String guidelineEn;
    private final String guidelineJa;

    /** 리스트/글머리표 기반 스타일 여부 */
    private final boolean listFriendly;
    /** 장문/서사형에 적합한지 여부 */
    private final boolean longFormFriendly;
    /** 환각/픽션 성향이 강해 사실형 작업에 주의가 필요한지 여부 */
    private final boolean highHallucinationRisk;

    StyleType(
            StyleAxis axis,
            String displayName,
            String guidelineKo,
            String guidelineEn,
            String guidelineJa,
            boolean listFriendly,
            boolean longFormFriendly,
            boolean highHallucinationRisk
    ) {
        this.axis = axis;
        this.displayName = displayName;
        this.guidelineKo = guidelineKo;
        this.guidelineEn = guidelineEn;
        this.guidelineJa = guidelineJa;
        this.listFriendly = listFriendly;
        this.longFormFriendly = longFormFriendly;
        this.highHallucinationRisk = highHallucinationRisk;
    }

    /**
     * 언어 타입에 따라 적절한 스타일 가이드라인을 반환한다.
     */
    public String getGuidelineByLang(LanguageType lang) {
        return getByLang(lang, guidelineKo, guidelineEn, guidelineJa);
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

