package org.example.sharedprompts.domain.prompt.common.enums.action.category.development;

import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

@Getter
public enum CloudServicesActionType implements ActionTypeInterface, StableKeyedEnum {
    CLOUD_DEPLOYMENT("ACTION.CLOUD_SERVICES.CLOUD_DEPLOYMENT", "클라우드 배포", "Cloud Deployment", "クラウドデプロイ", ActionGroup.CLOUD_DEPLOYMENT),
    CLOUD_SECURITY("ACTION.CLOUD_SERVICES.CLOUD_SECURITY", "클라우드 보안", "Cloud Security", "クラウドセキュリティ", ActionGroup.CLOUD_SECURITY),
    CLOUD_MONITORING("ACTION.CLOUD_SERVICES.CLOUD_MONITORING", "클라우드 모니터링", "Cloud Monitoring", "クラウド監視", ActionGroup.OBSERVABILITY),
    CLOUD_MIGRATION("ACTION.CLOUD_SERVICES.CLOUD_MIGRATION", "클라우드 마이그레이션", "Cloud Migration", "クラウドマイグレーション", ActionGroup.CLOUD_DEPLOYMENT),
    AWS_ARCHITECTURE("ACTION.CLOUD_SERVICES.AWS_ARCHITECTURE", "AWS 아키텍처", "AWS Architecture", "AWSアーキテクチャ", ActionGroup.CLOUD_PLATFORM_ARCHITECTURE),
    AZURE_SETUP("ACTION.CLOUD_SERVICES.AZURE_SETUP", "Azure 설정", "Azure Setup", "Azure設定", ActionGroup.CLOUD_DEPLOYMENT),
    GCP_CONFIGURATION("ACTION.CLOUD_SERVICES.GCP_CONFIGURATION", "GCP 구성", "GCP Configuration", "GCP構成", ActionGroup.CLOUD_DEPLOYMENT),
    CONTAINER_ORCHESTRATION("ACTION.CLOUD_SERVICES.CONTAINER_ORCHESTRATION", "컨테이너 오케스트레이션", "Container Orchestration", "コンテナオーケストレーション", ActionGroup.CLOUD_DEPLOYMENT),
    SERVERLESS_ARCHITECTURE("ACTION.CLOUD_SERVICES.SERVERLESS_ARCHITECTURE", "서버리스 아키텍처", "Serverless Architecture", "サーバーレスアーキテクチャ", ActionGroup.CLOUD_PLATFORM_ARCHITECTURE),
    CLOUD_COST_OPTIMIZATION("ACTION.CLOUD_SERVICES.CLOUD_COST_OPTIMIZATION", "클라우드 비용 최적화", "Cloud Cost Optimization", "クラウドコスト最適化", ActionGroup.CLOUD_COST_OPTIMIZATION),
    DISASTER_RECOVERY("ACTION.CLOUD_SERVICES.DISASTER_RECOVERY", "재해 복구", "Disaster Recovery", "災害復旧", ActionGroup.DISASTER_RECOVERY),
    CLOUD_BACKUP("ACTION.CLOUD_SERVICES.CLOUD_BACKUP", "클라우드 백업", "Cloud Backup", "クラウドバックアップ", ActionGroup.DISASTER_RECOVERY),
    LOAD_BALANCING("ACTION.CLOUD_SERVICES.LOAD_BALANCING", "로드 밸런싱", "Load Balancing", "ロードバランシング", ActionGroup.CAPACITY_SCALING),
    AUTO_SCALING("ACTION.CLOUD_SERVICES.AUTO_SCALING", "자동 스케일링", "Auto Scaling", "自動スケーリング", ActionGroup.CAPACITY_SCALING),
    CLOUD_NETWORKING("ACTION.CLOUD_SERVICES.CLOUD_NETWORKING", "클라우드 네트워킹", "Cloud Networking", "クラウドネットワーキング", ActionGroup.CLOUD_NETWORKING);

    private final String stableKey;
    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final ActionGroup actionGroup;

    CloudServicesActionType(String stableKey, String displayNameKo, String displayNameEn, String displayNameJa, ActionGroup actionGroup) {
        this.stableKey = stableKey;
        this.displayNameKo = displayNameKo;
        this.displayNameEn = displayNameEn;
        this.displayNameJa = displayNameJa;
        this.actionGroup = actionGroup;
    }

    @Override
    public String key() {
        return stableKey;
    }

    @Override
    public ActionGroup getActionGroup() {
        return actionGroup;
    }
}

