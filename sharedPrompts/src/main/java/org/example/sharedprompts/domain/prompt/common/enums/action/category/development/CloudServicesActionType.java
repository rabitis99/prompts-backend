package org.example.sharedprompts.domain.prompt.common.enums.action.category.development;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum CloudServicesActionType implements ActionTypeInterface, StableKeyedEnum {
    CLOUD_DEPLOYMENT("클라우드 배포", "Cloud Deployment", "クラウドデプロイ"),
    CLOUD_SECURITY("클라우드 보안", "Cloud Security", "クラウドセキュリティ"),
    CLOUD_MONITORING("클라우드 모니터링", "Cloud Monitoring", "クラウド監視"),
    CLOUD_MIGRATION("클라우드 마이그레이션", "Cloud Migration", "クラウドマイグレーション"),
    AWS_ARCHITECTURE("AWS 아키텍처", "AWS Architecture", "AWSアーキテクチャ"),
    AZURE_SETUP("Azure 설정", "Azure Setup", "Azure設定"),
    GCP_CONFIGURATION("GCP 구성", "GCP Configuration", "GCP構成"),
    CONTAINER_ORCHESTRATION("컨테이너 오케스트레이션", "Container Orchestration", "コンテナオーケストレーション"),
    SERVERLESS_ARCHITECTURE("서버리스 아키텍처", "Serverless Architecture", "サーバーレスアーキテクチャ"),
    CLOUD_COST_OPTIMIZATION("클라우드 비용 최적화", "Cloud Cost Optimization", "クラウドコスト最適化"),
    DISASTER_RECOVERY("재해 복구", "Disaster Recovery", "災害復旧"),
    CLOUD_BACKUP("클라우드 백업", "Cloud Backup", "クラウドバックアップ"),
    LOAD_BALANCING("로드 밸런싱", "Load Balancing", "ロードバランシング"),
    AUTO_SCALING("자동 스케일링", "Auto Scaling", "自動スケーリング"),
    CLOUD_NETWORKING("클라우드 네트워킹", "Cloud Networking", "クラウドネットワーキング");

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;

    @Override
    public String key() {
        return "ACTION.CLOUD_SERVICES." + name();
    }

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.TECHNICAL);
    }
}

