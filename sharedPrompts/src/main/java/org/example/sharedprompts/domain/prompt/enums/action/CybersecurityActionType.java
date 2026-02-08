package org.example.sharedprompts.domain.prompt.enums.action;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CybersecurityActionType implements ActionTypeInterface {
    VULNERABILITY_SCANNING("취약점 스캐닝", "Vulnerability Scanning", "脆弱性スキャン"),
    PENETRATION_TESTING("침투 테스트", "Penetration Testing", "ペネトレーションテスト"),
    THREAT_ANALYSIS("위협 분석", "Threat Analysis", "脅威分析"),
    DATA_ENCRYPTION("데이터 암호화", "Data Encryption", "データ暗号化"),
    SECURITY_AUDIT("보안 감사", "Security Audit", "セキュリティ監査"),
    INCIDENT_RESPONSE("사고 대응", "Incident Response", "インシデント対応"),
    SECURITY_POLICY("보안 정책", "Security Policy", "セキュリティポリシー"),
    ACCESS_CONTROL("접근 제어", "Access Control", "アクセス制御"),
    IDENTITY_MANAGEMENT("신원 관리", "Identity Management", "アイデンティティ管理"),
    NETWORK_SECURITY("네트워크 보안", "Network Security", "ネットワークセキュリティ"),
    APPLICATION_SECURITY("애플리케이션 보안", "Application Security", "アプリケーションセキュリティ"),
    SECURITY_TRAINING("보안 교육", "Security Training", "セキュリティ教育"),
    RISK_ASSESSMENT_SECURITY("보안 위험 평가", "Security Risk Assessment", "セキュリティリスク評価"),
    COMPLIANCE_MANAGEMENT("규정 준수 관리", "Compliance Management", "コンプライアンス管理"),
    SECURITY_MONITORING("보안 모니터링", "Security Monitoring", "セキュリティ監視"),
    MALWARE_ANALYSIS("멀웨어 분석", "Malware Analysis", "マルウェア分析"),
    FIREWALL_CONFIGURATION("방화벽 구성", "Firewall Configuration", "ファイアウォール構成");

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
}

