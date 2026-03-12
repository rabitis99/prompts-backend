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
public enum DevOpsActionType implements ActionTypeInterface, StableKeyedEnum {
    CI_CD_PIPELINE("CI/CD 파이프라인", "CI/CD Pipeline", "CI/CDパイプライン", OutputBehaviorType.CODE_IMPLEMENTATION),
    AUTOMATED_TESTING("자동화 테스트", "Automated Testing", "自動化テスト", OutputBehaviorType.CODE_IMPLEMENTATION),
    INTEGRATION_TESTING("통합 테스트", "Integration Testing", "統合テスト", OutputBehaviorType.CODE_IMPLEMENTATION),
    INFRASTRUCTURE_AS_CODE("코드로서의 인프라", "Infrastructure as Code", "コードとしてのインフラ", OutputBehaviorType.CODE_IMPLEMENTATION),
    CONTINUOUS_INTEGRATION("지속적 통합", "Continuous Integration", "継続的インテグレーション", OutputBehaviorType.CODE_IMPLEMENTATION),
    CONTINUOUS_DEPLOYMENT("지속적 배포", "Continuous Deployment", "継続的デプロイ", OutputBehaviorType.CODE_IMPLEMENTATION),
    CONTINUOUS_DELIVERY("지속적 전달", "Continuous Delivery", "継続的デリバリー", OutputBehaviorType.CODE_IMPLEMENTATION),
    CONTAINERIZATION("컨테이너화", "Containerization", "コンテナ化", OutputBehaviorType.CODE_IMPLEMENTATION),
    DOCKER_SETUP("Docker 설정", "Docker Setup", "Docker設定", OutputBehaviorType.CODE_IMPLEMENTATION),
    KUBERNETES_ORCHESTRATION("Kubernetes 오케스트레이션", "Kubernetes Orchestration", "Kubernetesオーケストレーション", OutputBehaviorType.CODE_IMPLEMENTATION),
    CONFIGURATION_MANAGEMENT("구성 관리", "Configuration Management", "構成管理", OutputBehaviorType.CODE_IMPLEMENTATION),
    VERSION_CONTROL("버전 관리", "Version Control", "バージョン管理", OutputBehaviorType.CODE_IMPLEMENTATION),
    DEPLOYMENT_STRATEGY_DEVOPS("배포 전략", "Deployment Strategy", "デプロイ戦略", OutputBehaviorType.CODE_IMPLEMENTATION),
    MONITORING_ALERTING("모니터링 및 알림", "Monitoring and Alerting", "監視とアラート", OutputBehaviorType.CODE_IMPLEMENTATION),
    LOG_MANAGEMENT("로그 관리", "Log Management", "ログ管理", OutputBehaviorType.CODE_IMPLEMENTATION),
    PERFORMANCE_MONITORING("성능 모니터링", "Performance Monitoring", "パフォーマンス監視", OutputBehaviorType.CODE_IMPLEMENTATION),
    INFRASTRUCTURE_MONITORING("인프라 모니터링", "Infrastructure Monitoring", "インフラ監視", OutputBehaviorType.CODE_IMPLEMENTATION);

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final OutputBehaviorType outputBehavior;

    @Override
    public String key() {
        return "ACTION.DEVOPS." + name();
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

