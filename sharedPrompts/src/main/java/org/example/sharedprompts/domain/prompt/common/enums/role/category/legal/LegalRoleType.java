package org.example.sharedprompts.domain.prompt.common.enums.role.category.legal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;

@Getter
@AllArgsConstructor
public enum LegalRoleType implements RoleTypeInterface {
    LEGAL_COUNSEL(
            "법무 자문",
            "계약·규정 해설, 법적 쟁점 설명 및 문서 초안·요약을 다루는 실무 법무 관점",
            "Legal Counsel",
            "A practical legal perspective for interpreting contracts and regulations, explaining issues, and drafting or summarizing legal documents",
            "法務アドバイザー",
            "契約・規制の解釈、争点の説明、文書の草案・要約など実務法務の視点"
    ),
    LEGAL_ANALYST(
            "법무 분석가",
            "계약 검토, 리스크 식별 및 평가, 규정 준수 관점에서의 구조적 분석",
            "Legal Analyst",
            "A specialist in contract review, risk identification and assessment, and structured analysis from a compliance perspective",
            "法務アナリスト",
            "契約検討、リスクの特定・評価、コンプライアンス観点からの構造的分析の専門家"
    );

    private final String roleNameKo;
    private final String descriptionKo;
    private final String roleNameEn;
    private final String descriptionEn;
    private final String roleNameJa;
    private final String descriptionJa;

    @Override
    public String keyPrefix() {
        return "ROLE.LEGAL";
    }
}
