package org.example.sharedprompts.domain.prompt.common.enums.action;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum DevOpsActionType implements ActionTypeInterface {
    CI_CD_PIPELINE("CI/CD 파이프라인", "CI/CD Pipeline", "CI/CDパイプライン"),
    AUTOMATED_TESTING("자동화 테스트", "Automated Testing", "自動化テスト"),
    INTEGRATION_TESTING("통합 테스트", "Integration Testing", "統合テスト"),
    INFRASTRUCTURE_AS_CODE("코드로서의 인프라", "Infrastructure as Code", "コードとしてのインフラ"),
    CONTINUOUS_INTEGRATION("지속적 통합", "Continuous Integration", "継続的インテグレーション"),
    CONTINUOUS_DEPLOYMENT("지속적 배포", "Continuous Deployment", "継続的デプロイ"),
    CONTINUOUS_DELIVERY("지속적 전달", "Continuous Delivery", "継続的デリバリー"),
    CONTAINERIZATION("컨테이너화", "Containerization", "コンテナ化"),
    DOCKER_SETUP("Docker 설정", "Docker Setup", "Docker設定"),
    KUBERNETES_ORCHESTRATION("Kubernetes 오케스트레이션", "Kubernetes Orchestration", "Kubernetesオーケストレーション"),
    CONFIGURATION_MANAGEMENT("구성 관리", "Configuration Management", "構成管理"),
    VERSION_CONTROL("버전 관리", "Version Control", "バージョン管理"),
    DEPLOYMENT_STRATEGY_DEVOPS("배포 전략", "Deployment Strategy", "デプロイ戦略"),
    MONITORING_ALERTING("모니터링 및 알림", "Monitoring and Alerting", "監視とアラート"),
    LOG_MANAGEMENT("로그 관리", "Log Management", "ログ管理"),
    PERFORMANCE_MONITORING("성능 모니터링", "Performance Monitoring", "パフォーマンス監視"),
    INFRASTRUCTURE_MONITORING("인프라 모니터링", "Infrastructure Monitoring", "インフラ監視");

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.TECHNICAL);
    }
}

