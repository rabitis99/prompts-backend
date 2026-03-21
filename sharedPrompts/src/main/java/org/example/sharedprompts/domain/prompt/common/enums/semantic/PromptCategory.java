package org.example.sharedprompts.domain.prompt.common.enums.semantic;

import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.engine.LanguageType;
import org.example.sharedprompts.domain.prompt.common.i18n.I18nKey;
import org.example.sharedprompts.domain.prompt.common.i18n.I18nRegistry;
import org.example.sharedprompts.domain.prompt.common.i18n.I18nText;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 프롬프트 엔진용 시맨틱 카테고리 식별자.
 * <p>UI 카테고리 선택, 역할 추천 게이팅, 시맨틱 해석 힌트에 사용된다.
 * key·i18n·defaultDomain 보유. StableKeyedEnum 계약으로 기존 키는 변경하지 않는다.</p>
 * <p>최종 안정 탑레벨: PRODUCTIVITY, BUSINESS, MARKETING, CUSTOMER_SUPPORT, DEVELOPMENT, DATA_ANALYSIS,
 * RESEARCH, EDUCATION, WRITING, DESIGN, LEGAL, EXTRACTION, ETC. 레거시(SALES, OPERATIONS, CONTENT_CREATION, ANALYSIS)는
 * 역직렬화 호환용 유지·canonical()로 정규화.</p>
 */
@Getter
public enum PromptCategory implements StableKeyedEnum {

    // ─── Canonical taxonomy (UI-selectable and semantic resolution) ─────────

    /** Task planning, schedule organization, workflow improvement, personal productivity systems. */
    PRODUCTIVITY(
            "PROMPT_CATEGORY.PRODUCTIVITY",
            I18nKey.of("prompt_category.productivity.display_name"),
            I18nKey.of("prompt_category.productivity.guideline"),
            TaskDomain.PRACTICAL
    ),

    BUSINESS(
            "PROMPT_CATEGORY.BUSINESS",
            I18nKey.of("prompt_category.business.display_name"),
            I18nKey.of("prompt_category.business.guideline"),
            TaskDomain.PRACTICAL
    ),

    MARKETING(
            "PROMPT_CATEGORY.MARKETING",
            I18nKey.of("prompt_category.marketing.display_name"),
            I18nKey.of("prompt_category.marketing.guideline"),
            TaskDomain.PRACTICAL
    ),

    /** 고객 지원·CS 업무 도메인. */
    CUSTOMER_SUPPORT(
            "PROMPT_CATEGORY.CUSTOMER_SUPPORT",
            I18nKey.of("prompt_category.customer_support.display_name"),
            I18nKey.of("prompt_category.customer_support.guideline"),
            TaskDomain.PRACTICAL
    ),

    /** 개발 전반(코딩/프로그래밍 상위). */
    DEVELOPMENT(
            "PROMPT_CATEGORY.DEVELOPMENT",
            I18nKey.of("prompt_category.development.display_name"),
            I18nKey.of("prompt_category.development.guideline"),
            TaskDomain.TECHNICAL
    ),

    /** 데이터 분석·인사이트 도출 도메인. */
    DATA_ANALYSIS(
            "PROMPT_CATEGORY.DATA_ANALYSIS",
            I18nKey.of("prompt_category.data_analysis.display_name"),
            I18nKey.of("prompt_category.data_analysis.guideline"),
            TaskDomain.ANALYTICAL
    ),

    RESEARCH(
            "PROMPT_CATEGORY.RESEARCH",
            I18nKey.of("prompt_category.research.display_name"),
            I18nKey.of("prompt_category.research.guideline"),
            TaskDomain.ANALYTICAL
    ),

    EDUCATION(
            "PROMPT_CATEGORY.EDUCATION",
            I18nKey.of("prompt_category.education.display_name"),
            I18nKey.of("prompt_category.education.guideline"),
            TaskDomain.EDUCATIONAL
    ),

    WRITING(
            "PROMPT_CATEGORY.WRITING",
            I18nKey.of("prompt_category.writing.display_name"),
            I18nKey.of("prompt_category.writing.guideline"),
            TaskDomain.CREATIVE
    ),

    DESIGN(
            "PROMPT_CATEGORY.DESIGN",
            I18nKey.of("prompt_category.design.display_name"),
            I18nKey.of("prompt_category.design.guideline"),
            TaskDomain.CREATIVE
    ),

    /** 법률·규정·계약 도메인. */
    LEGAL(
            "PROMPT_CATEGORY.LEGAL",
            I18nKey.of("prompt_category.legal.display_name"),
            I18nKey.of("prompt_category.legal.guideline"),
            TaskDomain.PRACTICAL
    ),

    /** 추출 모드 전용. ETC와 구분해 resolvedCategory·렌더링에서 추출 모드 보존. */
    EXTRACTION(
            "PROMPT_CATEGORY.EXTRACTION",
            I18nKey.of("prompt_category.extraction.display_name"),
            I18nKey.of("prompt_category.extraction.guideline"),
            TaskDomain.ANALYTICAL
    ),

    /** Fallback only; not primary UI choice. */
    ETC(
            "PROMPT_CATEGORY.ETC",
            I18nKey.of("prompt_category.etc.display_name"),
            I18nKey.of("prompt_category.etc.guideline"),
            TaskDomain.GENERAL
    ),

    // ─── Legacy (deserialization only; canonical() maps to above) ─────────────

    /** @deprecated Legacy; use {@link #MARKETING}. Stored values still deserialize as SALES. */
    SALES(
            "PROMPT_CATEGORY.SALES",
            I18nKey.of("prompt_category.sales.display_name"),
            I18nKey.of("prompt_category.sales.guideline"),
            TaskDomain.PRACTICAL
    ),

    /** @deprecated Legacy; use {@link #BUSINESS}. Stored values still deserialize as OPERATIONS. */
    OPERATIONS(
            "PROMPT_CATEGORY.OPERATIONS",
            I18nKey.of("prompt_category.operations.display_name"),
            I18nKey.of("prompt_category.operations.guideline"),
            TaskDomain.PRACTICAL
    ),

    /** @deprecated Legacy; use {@link #WRITING}. Stored values still deserialize as CONTENT_CREATION. */
    CONTENT_CREATION(
            "PROMPT_CATEGORY.CONTENT_CREATION",
            I18nKey.of("prompt_category.content_creation.display_name"),
            I18nKey.of("prompt_category.content_creation.guideline"),
            TaskDomain.CREATIVE
    ),

    /** Legacy; canonical is {@link #DATA_ANALYSIS}. Stored values still deserialize as ANALYSIS. */
    ANALYSIS(
            "PROMPT_CATEGORY.ANALYSIS",
            I18nKey.of("prompt_category.analysis.display_name"),
            I18nKey.of("prompt_category.analysis.guideline"),
            TaskDomain.ANALYTICAL
    ),

    /** Non-UI category; retained for backward compatibility. No canonical mapping. */
    CREATIVE(
            "PROMPT_CATEGORY.CREATIVE",
            I18nKey.of("prompt_category.creative.display_name"),
            I18nKey.of("prompt_category.creative.guideline"),
            TaskDomain.CREATIVE
    );

    /**
     * Canonical categories that must have a category semantic profile seed ({@link #canonical()} targets except {@link #EXTRACTION}).
     */
    private static final Set<PromptCategory> CANONICAL_SEMANTIC_PROFILE_CATEGORIES =
            computeCanonicalSemanticProfileCategories();

    private static Set<PromptCategory> computeCanonicalSemanticProfileCategories() {
        LinkedHashSet<PromptCategory> set = new LinkedHashSet<>();
        for (PromptCategory c : values()) {
            PromptCategory canon = c.canonical();
            if (canon != EXTRACTION) {
                set.add(canon);
            }
        }
        return Collections.unmodifiableSet(set);
    }

    public static Set<PromptCategory> canonicalSemanticProfileCategories() {
        return CANONICAL_SEMANTIC_PROFILE_CATEGORIES;
    }

    private final String key;
    private final I18nKey displayNameKey;
    private final I18nKey guidelineKey;
    /** 해석 힌트용 대표 TaskDomain. */
    private final TaskDomain defaultDomain;

    static {
        I18nRegistry registry = I18nRegistry.global();

        registry.register(I18nKey.of("prompt_category.productivity.display_name"),
                I18nText.of("생산성", "Productivity", "生産性"));
        registry.register(I18nKey.of("prompt_category.development.display_name"),
                I18nText.of("개발", "Development", "開発"));
        registry.register(I18nKey.of("prompt_category.analysis.display_name"),
                I18nText.of("분석", "Analysis", "分析"));
        registry.register(I18nKey.of("prompt_category.marketing.display_name"),
                I18nText.of("마케팅", "Marketing", "マーケティング"));
        registry.register(I18nKey.of("prompt_category.creative.display_name"),
                I18nText.of("창작", "Creative", "創作"));
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
        registry.register(I18nKey.of("prompt_category.sales.display_name"),
                I18nText.of("영업", "Sales", "営業"));
        registry.register(I18nKey.of("prompt_category.customer_support.display_name"),
                I18nText.of("고객 지원", "Customer Support", "カスタマーサポート"));
        registry.register(I18nKey.of("prompt_category.data_analysis.display_name"),
                I18nText.of("데이터 분석", "Data Analysis", "データ分析"));
        registry.register(I18nKey.of("prompt_category.content_creation.display_name"),
                I18nText.of("콘텐츠 제작", "Content Creation", "コンテンツ制作"));
        registry.register(I18nKey.of("prompt_category.operations.display_name"),
                I18nText.of("운영", "Operations", "運用"));
        registry.register(I18nKey.of("prompt_category.legal.display_name"),
                I18nText.of("법률", "Legal", "法務"));

        registry.register(I18nKey.of("prompt_category.productivity.guideline"),
                I18nText.of(
                        "업무 계획, 일정 정리, 워크플로 개선, 개인 생산성 시스템",
                        "Task planning, schedule organization, workflow improvement, personal productivity systems",
                        "タスク計画、スケジュール整理、ワークフロー改善、個人生産性システム"
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
        registry.register(I18nKey.of("prompt_category.creative.guideline"),
                I18nText.of(
                        "예술적 아이디어, 창작물 개발, 창의적 조언",
                        "Artistic ideas, creative work development, creative guidance",
                        "芸術的アイデア、作品開発、創造的助言"
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
        registry.register(I18nKey.of("prompt_category.sales.guideline"),
                I18nText.of(
                        "영업 전략, 제안서, 협상, 고객 관리",
                        "Sales strategy, proposals, negotiation, account management",
                        "営業戦略、提案書、交渉、顧客管理"
                ));
        registry.register(I18nKey.of("prompt_category.customer_support.guideline"),
                I18nText.of(
                        "고객 문의 대응, 티켓 처리, FAQ, 온보딩",
                        "Customer inquiry handling, ticket resolution, FAQ, onboarding",
                        "顧客問い合わせ対応、チケット解決、FAQ、オンボーディング"
                ));
        registry.register(I18nKey.of("prompt_category.data_analysis.guideline"),
                I18nText.of(
                        "데이터 분석, 통계, 인사이트 도출, 분석적 접근",
                        "Data analysis, statistics, insight extraction, analytical approach",
                        "データ分析、統計、洞察抽出、分析的アプローチ"
                ));
        registry.register(I18nKey.of("prompt_category.content_creation.guideline"),
                I18nText.of(
                        "콘텐츠 기획, 작성법, 아이디어 개발, 블로그/SNS/영상",
                        "Content planning, writing, idea generation, blogs/social media/video",
                        "コンテンツ企画、制作、アイデア開発、ブログ/SNS/動画"
                ));
        registry.register(I18nKey.of("prompt_category.operations.guideline"),
                I18nText.of(
                        "프로세스 개선, 운영 절차, 리소스 관리",
                        "Process improvement, operational procedures, resource management",
                        "プロセス改善、運用手順、リソース管理"
                ));
        registry.register(I18nKey.of("prompt_category.legal.guideline"),
                I18nText.of(
                        "계약 검토, 규정 해석, 법률 문건 작성",
                        "Contract review, regulation interpretation, legal document drafting",
                        "契約検討、規制解釈、法務文書作成"
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
     * Canonical category for semantic resolution. Legacy categories map to their canonical equivalent.
     * Use this when resolving profiles, role/action recommendation, or intent matching.
     */
    public PromptCategory canonical() {
        return switch (this) {
            case SALES -> MARKETING;
            case OPERATIONS -> BUSINESS;
            case CONTENT_CREATION -> WRITING;
            case ANALYSIS -> DATA_ANALYSIS;
            default -> this;
        };
    }

    /** True if this value is legacy (SALES, OPERATIONS, CONTENT_CREATION, ANALYSIS). */
    public boolean isLegacy() {
        return this == SALES || this == OPERATIONS || this == CONTENT_CREATION || this == ANALYSIS;
    }

    /**
     * True if this category should be offered in UI category selection.
     * EXTRACTION is system-only; ETC is fallback-only; legacy and CREATIVE are not UI-selectable.
     */
    public boolean isUiSelectable() {
        return this == PRODUCTIVITY || this == BUSINESS || this == MARKETING || this == CUSTOMER_SUPPORT
                || this == DEVELOPMENT || this == DATA_ANALYSIS || this == RESEARCH || this == EDUCATION
                || this == WRITING || this == DESIGN || this == LEGAL;
    }

    /** UI 기본 표시(한국어). */
    public String getDisplayName() {
        return I18nRegistry.global().lookup(displayNameKey, LanguageType.KOREAN);
    }

    public String getGuidelineByLang(LanguageType lang) {
        return I18nRegistry.global().lookup(guidelineKey, lang);
    }

    public String getGuidelineEn() {
        return getGuidelineByLang(LanguageType.ENGLISH);
    }

}
