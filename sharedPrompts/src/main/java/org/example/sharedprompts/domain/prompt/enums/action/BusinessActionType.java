package org.example.sharedprompts.domain.prompt.enums.action;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum BusinessActionType implements ActionTypeInterface {
    PROPOSAL_WRITING("기획서 작성", "Proposal Writing", "企画書作成"),
    REPORT_WRITING("보고서 작성", "Report Writing", "報告書作成"),
    BUSINESS_STRATEGY("사업 전략", "Business Strategy", "事業戦略"),
    PROJECT_MANAGEMENT("프로젝트 관리", "Project Management", "プロジェクト管理"),
    FINANCIAL_ANALYSIS("재무 분석", "Financial Analysis", "財務分析"),
    PRESENTATION_PREPARATION("발표 자료 준비", "Presentation Preparation", "プレゼンテーション資料準備"),
    CONTRACT_REVIEW("계약 검토", "Contract Review", "契約検討"),
    RISK_ASSESSMENT("리스크 평가", "Risk Assessment", "リスク評価"),
    STAKEHOLDER_MANAGEMENT("이해관계자 관리", "Stakeholder Management", "ステークホルダー管理"),
    BUSINESS_PLAN_DEVELOPMENT("사업계획서 개발", "Business Plan Development", "事業計画書開発");

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.PRACTICAL);
    }
}

