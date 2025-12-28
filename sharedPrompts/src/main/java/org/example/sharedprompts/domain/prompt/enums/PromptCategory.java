package org.example.sharedprompts.domain.prompt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PromptCategory {

    PRODUCTIVITY(
            "생산성",
            "업무 효율 향상, 시간 관리, 자동화 등 생산성을 높이는 전략과 아이디어를 제공합니다.",
            "Focus on improving productivity through workflow optimization, time management, and automation strategies.",
            "業務効率、時間管理、自動化など、生産性向上のための戦略やアイデアを提供してください。"
    ),

    DEVELOPMENT(
            "개발",
            "소프트웨어 개발 관련 지식, 기술 스택, 프로젝트 구조와 최적화 방법을 다룹니다.",
            "Provide expertise on software development, including technology stacks, system architecture, and optimization techniques.",
            "ソフトウェア開発に関する知識、技術スタック、プロジェクト構造、最適化手法を提供してください。"
    ),

    CODING(
            "코딩",
            "깨끗하고 유지보수 가능한 코드를 작성하며, 모범 사례와 디자인 패턴에 정통합니다. 복잡한 기술적 문제를 단순하고 우아하게 해결합니다.",
            "Write clean, maintainable code while applying best practices and design patterns to solve complex technical problems elegantly.",
            "クリーンで保守性の高いコードを作成し、ベストプラクティスやデザインパターンを活用して複雑な問題を解決してください。"
    ),

    PROGRAMMING(
            "프로그래밍",
            "프로그래밍 언어, 알고리즘, 자료구조 등 개발 전반에 대한 실용적 지식을 제공합니다.",
            "Provide practical knowledge across programming languages, algorithms, and data structures.",
            "プログラミング言語、アルゴリズム、データ構造など、開発全般の実用的な知識を提供してください。"
    ),

    ANALYSIS(
            "분석",
            "데이터 분석, 통계, 인사이트 도출 등 정보 해석과 문제 해결을 위한 분석적 접근을 다룹니다.",
            "Apply analytical approaches to data analysis, statistics, and insight extraction for problem solving.",
            "データ分析、統計、洞察の抽出など、問題解決のための分析的アプローチを行ってください。"
    ),

    MARKETING(
            "마케팅",
            "마케팅 전략, 브랜딩, 광고, 시장 조사 등을 포함하여 고객과 비즈니스 성장을 지원합니다.",
            "Support business growth through marketing strategies, branding, advertising, and market research.",
            "マーケティング戦略、ブランディング、広告、市場調査を通じてビジネス成長を支援してください。"
    ),

    CONTENT(
            "콘텐츠 제작",
            "블로그, SNS, 영상 등 다양한 매체에 맞는 콘텐츠 기획과 작성법, 아이디어 개발을 안내합니다.",
            "Guide content planning, writing, and idea generation tailored to blogs, social media, and video platforms.",
            "ブログ、SNS、動画などの媒体に適したコンテンツ企画・制作を支援してください。"
    ),

    CREATIVE(
            "창작",
            "예술적, 창의적 아이디어와 창작물 개발에 관한 조언과 실습 가이드를 제공합니다.",
            "Provide guidance and practical advice for developing creative and artistic ideas.",
            "芸術的・創造的なアイデアや作品開発のための助言と実践的ガイドを提供してください。"
    ),

    STUDY(
            "학습",
            "효율적인 학습 방법, 자료 정리, 이해력 향상 및 학습 계획 수립을 돕습니다.",
            "Help design effective study methods, learning plans, and knowledge organization.",
            "効率的な学習方法、資料整理、理解力向上、学習計画の立案を支援してください。"
    ),

    EDUCATION(
            "교육",
            "교육 자료 설계, 교수법, 학습자 맞춤형 교육 전략을 제공합니다.",
            "Design educational materials and teaching strategies tailored to learners.",
            "教育資料の設計、教授法、学習者に最適化された教育戦略を提供してください。"
    ),

    RESEARCH(
            "연구",
            "과학적, 학문적 연구 방법, 실험 설계, 논문 작성과 데이터 해석을 지원합니다.",
            "Support academic and scientific research including methodology, experiment design, and paper writing.",
            "研究方法、実験設計、論文作成、データ解析を支援してください。"
    ),

    BUSINESS(
            "비즈니스",
            "기획서 작성, 보고서, 사업 전략, 프로젝트 관리 등 실무 비즈니스 활동을 지원합니다.",
            "Assist with business activities such as proposals, reports, strategy planning, and project management.",
            "企画書、報告書、事業戦略、プロジェクト管理などの実務を支援してください。"
    ),

    DESIGN(
            "디자인",
            "UI/UX, 그래픽 디자인, 제품 디자인 등 사용자 경험을 고려한 설계를 다룹니다.",
            "Focus on user-centered design including UI/UX, graphic, and product design.",
            "UI/UX、グラフィック、プロダクトデザインなど、ユーザー体験を重視した設計を行ってください。"
    ),

    WRITING(
            "글쓰기",
            "다양한 형식의 글 작성과 효과적인 메시지 전달 방법을 안내합니다.",
            "Guide writing across formats with an emphasis on clarity and persuasive messaging.",
            "さまざまな文章形式において、明確で効果的な表現方法を指導してください。"
    ),

    ETC(
            "기타",
            "위 카테고리에 속하지 않는 다양한 주제와 아이디어를 포괄적으로 다룹니다.",
            "Handle a wide range of topics that do not fall into predefined categories.",
            "特定のカテゴリに属さない多様なトピックを包括的に扱ってください。"
    );

    /** UI / 표시용 */
    private final String displayName;

    /** Prompt Role Guideline */
    private final String guidelineKo;
    private final String guidelineEn;
    private final String guidelineJa;
}

