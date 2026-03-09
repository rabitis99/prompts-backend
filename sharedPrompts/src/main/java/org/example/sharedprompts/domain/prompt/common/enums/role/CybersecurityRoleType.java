package org.example.sharedprompts.domain.prompt.common.enums.role;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CybersecurityRoleType implements RoleTypeInterface {
    SECURITY_ENGINEER(
            "보안 엔지니어",
            "시스템 보안 설계 및 취약점 분석 전문가",
            "Security Engineer",
            "A specialist in system security design and vulnerability analysis",
            "セキュリティエンジニア",
            "システムセキュリティ設計と脆弱性分析の専門家"
    ),
    PENETRATION_TESTER(
            "침투 테스트 전문가",
            "보안 취약점 탐지 및 침투 테스트 수행 전문가",
            "Penetration Tester",
            "A specialist in security vulnerability detection and penetration testing",
            "ペネトレーションテスト専門家",
            "セキュリティ脆弱性検出とペネトレーションテスト実施の専門家"
    ),
    SECURITY_ANALYST(
            "보안 분석가",
            "위협 분석 및 보안 사고 대응 전문가",
            "Security Analyst",
            "A specialist in threat analysis and security incident response",
            "セキュリティアナリスト",
            "脅威分析とセキュリティインシデント対応の専門家"
    ),
    INFORMATION_SECURITY_OFFICER(
            "정보 보안 담당자",
            "정보 보안 정책 수립 및 관리 전문가",
            "Information Security Officer",
            "A specialist in information security policy establishment and management",
            "情報セキュリティ担当者",
            "情報セキュリティポリシー策定と管理の専門家"
    );

    private final String roleNameKo;
    private final String descriptionKo;
    private final String roleNameEn;
    private final String descriptionEn;
    private final String roleNameJa;
    private final String descriptionJa;

    @Override
    public String keyPrefix() {
        return "ROLE.CYBERSECURITY";
    }
}

