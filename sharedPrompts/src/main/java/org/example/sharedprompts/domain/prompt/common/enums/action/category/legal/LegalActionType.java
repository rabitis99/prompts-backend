package org.example.sharedprompts.domain.prompt.common.enums.action.category.legal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

@Getter
@AllArgsConstructor
public enum LegalActionType implements ActionTypeInterface, StableKeyedEnum {
    CONTRACT_REVIEW(
            "ACTION.LEGAL.CONTRACT_REVIEW",
            "계약 검토",
            "Contract Review",
            "契約検討",
            ActionGroup.EVALUATION_OR_AUDIT
    ),
    RISK_ASSESSMENT(
            "ACTION.LEGAL.RISK_ASSESSMENT",
            "법적 리스크 평가",
            "Legal Risk Assessment",
            "法的リスク評価",
            ActionGroup.RISK_ASSESSMENT
    ),
    LEGAL_PROPOSAL_DRAFTING(
            "ACTION.LEGAL.LEGAL_PROPOSAL_DRAFTING",
            "법무 제안·계약안 초안",
            "Legal Proposal or Agreement Drafting",
            "法務提案・契約案草案",
            ActionGroup.PROJECT_OR_BUSINESS_PLANNING
    ),
    LEGAL_REPORT_OR_MEMO(
            "ACTION.LEGAL.LEGAL_REPORT_OR_MEMO",
            "법무 보고서·메모",
            "Legal Report or Memo",
            "法務報告・メモ",
            ActionGroup.PRESENTATION_OR_REPORT
    );

    private final String stableKey;
    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final ActionGroup actionGroup;

    @Override
    public String key() {
        return stableKey;
    }

    @Override
    public ActionGroup getActionGroup() {
        return actionGroup;
    }
}
