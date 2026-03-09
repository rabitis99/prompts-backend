package org.example.sharedprompts.domain.prompt.common.enums.role;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.enums.StableKeyedEnum;

@Getter
@AllArgsConstructor
public enum BusinessRoleType implements RoleTypeInterface, StableKeyedEnum {
    BUSINESS_CONSULTANT(
            "비즈니스 컨설턴트",
            "사업 전략 수립 및 비즈니스 문제 해결 전문가",
            "Business Consultant",
            "A specialist in business strategy planning and business problem solving",
            "ビジネスコンサルタント",
            "事業戦略策定とビジネス問題解決の専門家"
    ),
    PROJECT_MANAGER(
            "프로젝트 매니저",
            "프로젝트 계획 수립 및 일정 관리 전문가",
            "Project Manager",
            "A specialist in project planning and schedule management",
            "プロジェクトマネージャー",
            "プロジェクト計画策定とスケジュール管理の専門家"
    ),
    BUSINESS_ANALYST_BUSINESS(
            "비즈니스 분석가",
            "비즈니스 요구사항 분석 및 솔루션 제안 전문가",
            "Business Analyst",
            "A specialist who analyzes business requirements and proposes solutions",
            "ビジネスアナリスト",
            "ビジネス要件分析とソリューション提案の専門家"
    ),
    FINANCIAL_ANALYST(
            "재무 분석가",
            "재무 분석 및 재무 전략 수립 전문가",
            "Financial Analyst",
            "A specialist in financial analysis and financial strategy planning",
            "財務アナリスト",
            "財務分析と財務戦略策定の専門家"
    );

    private final String roleNameKo;
    private final String descriptionKo;
    private final String roleNameEn;
    private final String descriptionEn;
    private final String roleNameJa;
    private final String descriptionJa;

    @Override
    public String keyPrefix() {
        return "ROLE.BUSINESS";
    }
}

