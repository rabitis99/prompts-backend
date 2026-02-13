package org.example.sharedprompts.domain.prompt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PromptCategory {

    PRODUCTIVITY(
            "생산성",
            "업무 효율 향상, 시간 관리, 자동화 전략",
            "Workflow optimization, time management, automation strategies",
            "業務効率向上、時間管理、自動化戦略",
            TaskDomain.PRACTICAL
    ),

    DEVELOPMENT(
            "개발",
            "소프트웨어 개발, 기술 스택, 프로젝트 구조, 최적화",
            "Software development, technology stacks, system architecture, optimization",
            "ソフトウェア開発、技術スタック、プロジェクト構造、最適化",
            TaskDomain.TECHNICAL
    ),

    CODING(
            "코딩",
            "깨끗한 코드, 모범 사례, 디자인 패턴, 문제 해결",
            "Clean code, best practices, design patterns, problem solving",
            "クリーンなコード、ベストプラクティス、デザインパターン、問題解決",
            TaskDomain.TECHNICAL
    ),

    PROGRAMMING(
            "프로그래밍",
            "프로그래밍 언어, 알고리즘, 자료구조, 실용적 지식",
            "Programming languages, algorithms, data structures, practical knowledge",
            "プログラミング言語、アルゴリズム、データ構造、実用的知識",
            TaskDomain.TECHNICAL
    ),

    ANALYSIS(
            "분석",
            "데이터 분석, 통계, 인사이트 도출, 분석적 접근",
            "Data analysis, statistics, insight extraction, analytical approach",
            "データ分析、統計、洞察抽出、分析的アプローチ",
            TaskDomain.ANALYTICAL
    ),

    MARKETING(
            "마케팅",
            "마케팅 전략, 브랜딩, 광고, 시장 조사, 비즈니스 성장",
            "Marketing strategies, branding, advertising, market research, business growth",
            "マーケティング戦略、ブランディング、広告、市場調査、ビジネス成長",
            TaskDomain.PRACTICAL
    ),

    CONTENT(
            "콘텐츠 제작",
            "콘텐츠 기획, 작성법, 아이디어 개발, 블로그/SNS/영상",
            "Content planning, writing, idea generation, blogs/social media/video",
            "コンテンツ企画、制作、アイデア開発、ブログ/SNS/動画",
            TaskDomain.CREATIVE
    ),

    CREATIVE(
            "창작",
            "예술적 아이디어, 창작물 개발, 창의적 조언",
            "Artistic ideas, creative work development, creative guidance",
            "芸術的アイデア、作品開発、創造的助言",
            TaskDomain.CREATIVE
    ),

    STUDY(
            "학습",
            "효율적 학습 방법, 자료 정리, 이해력 향상, 학습 계획",
            "Effective study methods, knowledge organization, learning plans",
            "効率的学習方法、資料整理、理解力向上、学習計画",
            TaskDomain.EDUCATIONAL
    ),

    EDUCATION(
            "교육",
            "교육 자료 설계, 교수법, 맞춤형 교육 전략",
            "Educational materials design, teaching methods, tailored strategies",
            "教育資料設計、教授法、最適化された教育戦略",
            TaskDomain.EDUCATIONAL
    ),

    RESEARCH(
            "연구",
            "연구 방법, 실험 설계, 논문 작성, 데이터 해석",
            "Research methodology, experiment design, paper writing, data interpretation",
            "研究方法、実験設計、論文作成、データ解析",
            TaskDomain.ANALYTICAL
    ),

    BUSINESS(
            "비즈니스",
            "기획서 작성, 보고서, 사업 전략, 프로젝트 관리",
            "Proposals, reports, business strategy, project management",
            "企画書、報告書、事業戦略、プロジェクト管理",
            TaskDomain.PRACTICAL
    ),

    DESIGN(
            "디자인",
            "UI/UX, 그래픽 디자인, 제품 디자인, 사용자 경험 중심",
            "UI/UX, graphic design, product design, user-centered design",
            "UI/UX、グラフィックデザイン、プロダクトデザイン、ユーザー体験重視",
            TaskDomain.CREATIVE
    ),

    WRITING(
            "글쓰기",
            "다양한 형식 글쓰기, 효과적 메시지 전달",
            "Writing across formats, clear and persuasive messaging",
            "多様な文章形式、明確で効果的な表現",
            TaskDomain.CREATIVE
    ),

    ETC(
            "기타",
            "다양한 주제와 아이디어, 포괄적 접근",
            "Wide range of topics, comprehensive approach",
            "多様なトピック、包括的アプローチ",
            TaskDomain.GENERAL
    );

    /** UI / 표시용 */
    private final String displayName;

    /** Prompt Role Guideline */
    private final String guidelineKo;
    private final String guidelineEn;
    private final String guidelineJa;

    /** 대표 TaskDomain (힌트 — ActionType이 override 가능) */
    private final TaskDomain defaultDomain;
}
