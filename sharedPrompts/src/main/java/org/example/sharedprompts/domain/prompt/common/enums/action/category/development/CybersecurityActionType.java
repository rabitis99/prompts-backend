package org.example.sharedprompts.domain.prompt.common.enums.action.category.development;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum CybersecurityActionType implements ActionTypeInterface, StableKeyedEnum {
    VULNERABILITY_SCANNING("취약점 스캐닝", "Vulnerability Scanning", "脆弱性スキャン", OutputBehaviorType.CODE_IMPLEMENTATION),
    PENETRATION_TESTING("침투 테스트", "Penetration Testing", "ペネトレーションテスト", OutputBehaviorType.CODE_IMPLEMENTATION),
    THREAT_ANALYSIS("위협 분석", "Threat Analysis", "脅威分析", OutputBehaviorType.CODE_IMPLEMENTATION),
    DATA_ENCRYPTION("데이터 암호화", "Data Encryption", "データ暗号化", OutputBehaviorType.CODE_IMPLEMENTATION),
    SECURITY_AUDIT("보안 감사", "Security Audit", "セキュリティ監査", OutputBehaviorType.CODE_IMPLEMENTATION),
    INCIDENT_RESPONSE("사고 대응", "Incident Response", "インシデント対応", OutputBehaviorType.CODE_IMPLEMENTATION),
    SECURITY_POLICY("보안 정책", "Security Policy", "セキュリティポリシー", OutputBehaviorType.CODE_IMPLEMENTATION),
    ACCESS_CONTROL("접근 제어", "Access Control", "アクセス制御", OutputBehaviorType.CODE_IMPLEMENTATION),
    IDENTITY_MANAGEMENT("신원 관리", "Identity Management", "アイデンティティ管理", OutputBehaviorType.CODE_IMPLEMENTATION),
    NETWORK_SECURITY("네트워크 보안", "Network Security", "ネットワークセキュリティ", OutputBehaviorType.CODE_IMPLEMENTATION),
    APPLICATION_SECURITY("애플리케이션 보안", "Application Security", "アプリケーションセキュリティ", OutputBehaviorType.CODE_IMPLEMENTATION),
    SECURITY_TRAINING("보안 교육", "Security Training", "セキュリティ教育", OutputBehaviorType.CODE_IMPLEMENTATION),
    RISK_ASSESSMENT_SECURITY("보안 위험 평가", "Security Risk Assessment", "セキュリティリスク評価", OutputBehaviorType.CODE_IMPLEMENTATION),
    COMPLIANCE_MANAGEMENT("규정 준수 관리", "Compliance Management", "コンプライアンス管理", OutputBehaviorType.CODE_IMPLEMENTATION),
    SECURITY_MONITORING("보안 모니터링", "Security Monitoring", "セキュリティ監視", OutputBehaviorType.CODE_IMPLEMENTATION),
    MALWARE_ANALYSIS("멀웨어 분석", "Malware Analysis", "マルウェア分析", OutputBehaviorType.CODE_IMPLEMENTATION),
    FIREWALL_CONFIGURATION("방화벽 구성", "Firewall Configuration", "ファイアウォール構成", OutputBehaviorType.CODE_IMPLEMENTATION);

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final OutputBehaviorType outputBehavior;

    @Override
    public String key() {
        return "ACTION.CYBERSECURITY." + name();
    }

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.TECHNICAL);
    }

    @Override
    public OutputBehaviorType getOutputBehavior() {
        return outputBehavior;
    }
}

