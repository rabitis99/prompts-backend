package org.example.sharedprompts.domain.prompt.common.enums.action.category.development;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

@Getter
@AllArgsConstructor
public enum DevOpsActionType implements ActionTypeInterface, StableKeyedEnum {
    CI_CD_PIPELINE("CI/CD 파이프라인", "CI/CD Pipeline", "CI/CDパイプライン", ActionGroup.DELIVERY_AUTOMATION),
    AUTOMATED_TESTING("자동화 테스트", "Automated Testing", "自動化テスト", ActionGroup.DELIVERY_AUTOMATION),
    INTEGRATION_TESTING("통합 테스트", "Integration Testing", "統合テスト", ActionGroup.DELIVERY_AUTOMATION),
    INFRASTRUCTURE_AS_CODE("코드로서의 인프라", "Infrastructure as Code", "コードとしてのインフラ", ActionGroup.INFRASTRUCTURE_AS_CODE),
    CONTINUOUS_INTEGRATION("지속적 통합", "Continuous Integration", "継続的インテグレーション", ActionGroup.DELIVERY_AUTOMATION),
    CONTINUOUS_DEPLOYMENT("지속적 배포", "Continuous Deployment", "継続的デプロイ", ActionGroup.DELIVERY_AUTOMATION),
    CONTINUOUS_DELIVERY("지속적 전달", "Continuous Delivery", "継続的デリバリー", ActionGroup.DELIVERY_AUTOMATION),
    CONTAINERIZATION("컨테이너화", "Containerization", "コンテナ化", ActionGroup.INFRASTRUCTURE_AS_CODE),
    DOCKER_SETUP("Docker 설정", "Docker Setup", "Docker設定", ActionGroup.INFRASTRUCTURE_AS_CODE),
    KUBERNETES_ORCHESTRATION("Kubernetes 오케스트레이션", "Kubernetes Orchestration", "Kubernetesオーケストレーション", ActionGroup.INFRASTRUCTURE_AS_CODE),
    CONFIGURATION_MANAGEMENT("구성 관리", "Configuration Management", "構成管理", ActionGroup.INFRASTRUCTURE_AS_CODE),
    VERSION_CONTROL("버전 관리", "Version Control", "バージョン管理", ActionGroup.DELIVERY_AUTOMATION),
    DEPLOYMENT_STRATEGY_DEVOPS("배포 전략", "Deployment Strategy", "デプロイ戦略", ActionGroup.STRATEGY),
    MONITORING_ALERTING("모니터링 및 알림", "Monitoring and Alerting", "監視とアラート", ActionGroup.OBSERVABILITY),
    LOG_MANAGEMENT("로그 관리", "Log Management", "ログ管理", ActionGroup.OBSERVABILITY),
    PERFORMANCE_MONITORING("성능 모니터링", "Performance Monitoring", "パフォーマンス監視", ActionGroup.OBSERVABILITY),
    INFRASTRUCTURE_MONITORING("인프라 모니터링", "Infrastructure Monitoring", "インフラ監視", ActionGroup.OBSERVABILITY);

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final ActionGroup actionGroup;

    @Override
    public String key() {
        return "ACTION.DEVOPS." + name();
    }

    @Override
    public ActionGroup getActionGroup() {
        return actionGroup;
    }
}

