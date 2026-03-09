package org.example.sharedprompts.domain.prompt.common.enums;

import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.i18n.I18nKey;
import org.example.sharedprompts.domain.prompt.common.i18n.I18nRegistry;
import org.example.sharedprompts.domain.prompt.common.i18n.I18nText;

/**
 * Prompt category — semantic identity for the prompt engine.
 *
 * <p>Identity: key, optional i18n keys for display/guideline (lookup via {@link I18nRegistry}).
 * defaultDomain is stable classification hint for resolution; routing and compatibility live in
 * {@link org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfile} and profile registries.</p>
 */
@Getter
public enum PromptCategory implements StableKeyedEnum {

    PRODUCTIVITY(
            "PROMPT_CATEGORY.PRODUCTIVITY",
            I18nKey.of("prompt_category.productivity.display_name"),
            I18nKey.of("prompt_category.productivity.guideline"),
            TaskDomain.PRACTICAL
    ),

    /**
     * 개발 전반 (코딩/프로그래밍을 포함하는 상위 카테고리).
     * <p>세분화된 레거시 카테고리 대신 대표 개발 카테고리로 사용한다.</p>
     */
    DEVELOPMENT(
            "PROMPT_CATEGORY.DEVELOPMENT",
            I18nKey.of("prompt_category.development.display_name"),
            I18nKey.of("prompt_category.development.guideline"),
            TaskDomain.TECHNICAL
    ),


    ANALYSIS(
            "PROMPT_CATEGORY.ANALYSIS",
            I18nKey.of("prompt_category.analysis.display_name"),
            I18nKey.of("prompt_category.analysis.guideline"),
            TaskDomain.ANALYTICAL
    ),

    MARKETING(
            "PROMPT_CATEGORY.MARKETING",
            I18nKey.of("prompt_category.marketing.display_name"),
            I18nKey.of("prompt_category.marketing.guideline"),
            TaskDomain.PRACTICAL
    ),

    CONTENT(
            "PROMPT_CATEGORY.CONTENT",
            I18nKey.of("prompt_category.content.display_name"),
            I18nKey.of("prompt_category.content.guideline"),
            TaskDomain.CREATIVE
    ),

    CREATIVE(
            "PROMPT_CATEGORY.CREATIVE",
            I18nKey.of("prompt_category.creative.display_name"),
            I18nKey.of("prompt_category.creative.guideline"),
            TaskDomain.CREATIVE
    ),

    STUDY(
            "PROMPT_CATEGORY.STUDY",
            I18nKey.of("prompt_category.study.display_name"),
            I18nKey.of("prompt_category.study.guideline"),
            TaskDomain.EDUCATIONAL
    ),

    EDUCATION(
            "PROMPT_CATEGORY.EDUCATION",
            I18nKey.of("prompt_category.education.display_name"),
            I18nKey.of("prompt_category.education.guideline"),
            TaskDomain.EDUCATIONAL
    ),

    RESEARCH(
            "PROMPT_CATEGORY.RESEARCH",
            I18nKey.of("prompt_category.research.display_name"),
            I18nKey.of("prompt_category.research.guideline"),
            TaskDomain.ANALYTICAL
    ),

    BUSINESS(
            "PROMPT_CATEGORY.BUSINESS",
            I18nKey.of("prompt_category.business.display_name"),
            I18nKey.of("prompt_category.business.guideline"),
            TaskDomain.PRACTICAL
    ),

    DESIGN(
            "PROMPT_CATEGORY.DESIGN",
            I18nKey.of("prompt_category.design.display_name"),
            I18nKey.of("prompt_category.design.guideline"),
            TaskDomain.CREATIVE
    ),

    WRITING(
            "PROMPT_CATEGORY.WRITING",
            I18nKey.of("prompt_category.writing.display_name"),
            I18nKey.of("prompt_category.writing.guideline"),
            TaskDomain.CREATIVE
    ),

    /**
     * 추출 모드 전용 — 카테고리 미지정/별도 모드로, 구조화된 데이터 추출 요청에 사용.
     * ETC와 구분하여 resolvedCategory·렌더링·집계에서 추출 모드임을 보존한다.
     */
    EXTRACTION(
            "PROMPT_CATEGORY.EXTRACTION",
            I18nKey.of("prompt_category.extraction.display_name"),
            I18nKey.of("prompt_category.extraction.guideline"),
            TaskDomain.ANALYTICAL
    ),

    ETC(
            "PROMPT_CATEGORY.ETC",
            I18nKey.of("prompt_category.etc.display_name"),
            I18nKey.of("prompt_category.etc.guideline"),
            TaskDomain.GENERAL
    );

    /** Stable serialization-safe identifier */
    private final String key;

    /** UI / 표시용 — 다국어 키 */
    private final I18nKey displayNameKey;

    /** Prompt Role Guideline — 다국어 키 */
    private final I18nKey guidelineKey;

    /** 대표 TaskDomain (힌트 — ActionType이 override 가능) */
    private final TaskDomain defaultDomain;

    static {
        I18nRegistry registry = I18nRegistry.global();

        // Display names
        registry.register(I18nKey.of("prompt_category.productivity.display_name"),
                I18nText.of("생산성", "Productivity", "生産性"));
        registry.register(I18nKey.of("prompt_category.development.display_name"),
                I18nText.of("개발", "Development", "開発"));
        registry.register(I18nKey.of("prompt_category.analysis.display_name"),
                I18nText.of("분석", "Analysis", "分析"));
        registry.register(I18nKey.of("prompt_category.marketing.display_name"),
                I18nText.of("마케팅", "Marketing", "マーケティング"));
        registry.register(I18nKey.of("prompt_category.content.display_name"),
                I18nText.of("콘텐츠 제작", "Content Creation", "コンテンツ制作"));
        registry.register(I18nKey.of("prompt_category.creative.display_name"),
                I18nText.of("창작", "Creative", "創作"));
        registry.register(I18nKey.of("prompt_category.study.display_name"),
                I18nText.of("학습", "Study", "学習"));
        registry.register(I18nKey.of("prompt_category.education.display_name"),
                I18nText.of("교육", "Education", "教育"));
        registry.register(I18nKey.of("prompt_category.research.display_name"),
                I18nText.of("연구", "Research", "研究"));
        registry.register(I18nKey.of("prompt_category.business.display_name"),
                I18nText.of("비즈니스", "Business", "ビジネス"));
        registry.register(I18nKey.of("prompt_category.design.display_name"),
                I18nText.of("디자인", "Design", "デザイン"));
        registry.register(I18nKey.of("prompt_category.writing.display_name"),
                I18nText.of("글쓰기", "Writing", "ライティング"));
        registry.register(I18nKey.of("prompt_category.extraction.display_name"),
                I18nText.of("추출", "Extraction", "抽出"));
        registry.register(I18nKey.of("prompt_category.etc.display_name"),
                I18nText.of("기타", "Etc", "その他"));

        // Guidelines (기존 ko/en/ja 텍스트 그대로 등록)
        registry.register(I18nKey.of("prompt_category.productivity.guideline"),
                I18nText.of(
                        "업무 효율 향상, 시간 관리, 자동화 전략",
                        "Workflow optimization, time management, automation strategies",
                        "業務効率向上、時間管理、自動化戦略"
                ));
        registry.register(I18nKey.of("prompt_category.development.guideline"),
                I18nText.of(
                        "소프트웨어 개발, 기술 스택, 프로젝트 구조, 최적화",
                        "Software development, technology stacks, system architecture, optimization",
                        "ソフトウェア開発、技術スタック、プロジェクト構造、最適化"
                ));
        registry.register(I18nKey.of("prompt_category.analysis.guideline"),
                I18nText.of(
                        "데이터 분석, 통계, 인사이트 도출, 분석적 접근",
                        "Data analysis, statistics, insight extraction, analytical approach",
                        "データ分析、統計、洞察抽出、分析的アプローチ"
                ));
        registry.register(I18nKey.of("prompt_category.marketing.guideline"),
                I18nText.of(
                        "마케팅 전략, 브랜딩, 광고, 시장 조사, 비즈니스 성장",
                        "Marketing strategies, branding, advertising, market research, business growth",
                        "マーケティング戦略、ブランディング、広告、市場調査、ビジネス成長"
                ));
        registry.register(I18nKey.of("prompt_category.content.guideline"),
                I18nText.of(
                        "콘텐츠 기획, 작성법, 아이디어 개발, 블로그/SNS/영상",
                        "Content planning, writing, idea generation, blogs/social media/video",
                        "コンテンツ企画、制作、アイデア開発、ブログ/SNS/動画"
                ));
        registry.register(I18nKey.of("prompt_category.creative.guideline"),
                I18nText.of(
                        "예술적 아이디어, 창작물 개발, 창의적 조언",
                        "Artistic ideas, creative work development, creative guidance",
                        "芸術的アイデア、作品開発、創造的助言"
                ));
        registry.register(I18nKey.of("prompt_category.study.guideline"),
                I18nText.of(
                        "효율적 학습 방법, 자료 정리, 이해력 향상, 학습 계획",
                        "Effective study methods, knowledge organization, learning plans",
                        "効率的学習方法、資料整理、理解力向上、学習計画"
                ));
        registry.register(I18nKey.of("prompt_category.education.guideline"),
                I18nText.of(
                        "교육 자료 설계, 교수법, 맞춤형 교육 전략",
                        "Educational materials design, teaching methods, tailored strategies",
                        "教育資料設計、教授法、最適化された教育戦略"
                ));
        registry.register(I18nKey.of("prompt_category.research.guideline"),
                I18nText.of(
                        "연구 방법, 실험 설계, 논문 작성, 데이터 해석",
                        "Research methodology, experiment design, paper writing, data interpretation",
                        "研究方法、実験設計、論文作成、データ解析"
                ));
        registry.register(I18nKey.of("prompt_category.business.guideline"),
                I18nText.of(
                        "기획서 작성, 보고서, 사업 전략, 프로젝트 관리",
                        "Proposals, reports, business strategy, project management",
                        "企画書、報告書、事業戦略、プロジェクト管理"
                ));
        registry.register(I18nKey.of("prompt_category.design.guideline"),
                I18nText.of(
                        "UI/UX, 그래픽 디자인, 제품 디자인, 사용자 경험 중심",
                        "UI/UX, graphic design, product design, user-centered design",
                        "UI/UX、グラフィックデザイン、プロダクトデザイン、ユーザー体験重視"
                ));
        registry.register(I18nKey.of("prompt_category.writing.guideline"),
                I18nText.of(
                        "다양한 형식 글쓰기, 효과적 메시지 전달",
                        "Writing across formats, clear and persuasive messaging",
                        "多様な文章形式、明確で効果的な表現"
                ));
        registry.register(I18nKey.of("prompt_category.extraction.guideline"),
                I18nText.of(
                        "구조화된 데이터 추출, JSON 스키마 기반 출력",
                        "Structured data extraction, JSON schema-based output",
                        "構造化データ抽出、JSONスキーマに基づく出力"
                ));
        registry.register(I18nKey.of("prompt_category.etc.guideline"),
                I18nText.of(
                        "다양한 주제와 아이디어, 포괄적 접근",
                        "Wide range of topics, comprehensive approach",
                        "多様なトピック、包括的アプローチ"
                ));
    }

    PromptCategory(String key, I18nKey displayNameKey, I18nKey guidelineKey, TaskDomain defaultDomain) {
        this.key = key;
        this.displayNameKey = displayNameKey;
        this.guidelineKey = guidelineKey;
        this.defaultDomain = defaultDomain;
    }

    @Override
    public String key() {
        return key;
    }

    /**
     * UI 기본 표시는 한국어를 사용한다.
     * (과거 displayName 필드의 값을 그대로 유지하는 동작)
     */
    public String getDisplayName() {
        return I18nRegistry.global().lookup(displayNameKey, LanguageType.KOREAN);
    }

    /**
     * 언어 타입에 따라 PromptCategory 가이드라인을 반환한다.
     * 현재는 GuidelineRenderer에서 직접 사용하지 않지만,
     * 엔드포인트/메타데이터 확장 시 재사용 가능하도록 제공한다.
     */
    public String getGuidelineByLang(LanguageType lang) {
        return I18nRegistry.global().lookup(guidelineKey, lang);
    }

    /**
     * 영어 기준 카테고리 가이드라인을 반환한다.
     * (기존 호출부의 getGuidelineEn()을 대체하기 위한 헬퍼)
     */
    public String getGuidelineEn() {
        return getGuidelineByLang(LanguageType.ENGLISH);
    }

    // 레거시 세분화 값(CODING, PROGRAMMING 등)은 제거되었으며,
    // 필요한 경우 상위 카테고리(DEVELOPMENT, CONTENT, EDUCATION 등)만 사용한다.
}
