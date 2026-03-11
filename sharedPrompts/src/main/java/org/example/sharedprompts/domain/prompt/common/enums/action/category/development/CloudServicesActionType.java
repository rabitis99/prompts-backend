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
public enum CloudServicesActionType implements ActionTypeInterface, StableKeyedEnum {
    CLOUD_DEPLOYMENT("클라우드 배포", "Cloud Deployment", "クラウドデプロイ", OutputBehaviorType.CODE_IMPLEMENTATION),
    CLOUD_SECURITY("클라우드 보안", "Cloud Security", "クラウドセキュリティ", OutputBehaviorType.CODE_IMPLEMENTATION),
    CLOUD_MONITORING("클라우드 모니터링", "Cloud Monitoring", "クラウド監視", OutputBehaviorType.CODE_IMPLEMENTATION),
    CLOUD_MIGRATION("클라우드 마이그레이션", "Cloud Migration", "クラウドマイグレーション", OutputBehaviorType.CODE_IMPLEMENTATION),
    AWS_ARCHITECTURE("AWS 아키텍처", "AWS Architecture", "AWSアーキテクチャ", OutputBehaviorType.CODE_IMPLEMENTATION),
    AZURE_SETUP("Azure 설정", "Azure Setup", "Azure設定", OutputBehaviorType.CODE_IMPLEMENTATION),
    GCP_CONFIGURATION("GCP 구성", "GCP Configuration", "GCP構成", OutputBehaviorType.CODE_IMPLEMENTATION),
    CONTAINER_ORCHESTRATION("컨테이너 오케스트레이션", "Container Orchestration", "コンテナオーケストレーション", OutputBehaviorType.CODE_IMPLEMENTATION),
    SERVERLESS_ARCHITECTURE("서버리스 아키텍처", "Serverless Architecture", "サーバーレスアーキテクチャ", OutputBehaviorType.CODE_IMPLEMENTATION),
    CLOUD_COST_OPTIMIZATION("클라우드 비용 최적화", "Cloud Cost Optimization", "クラウドコスト最適化", OutputBehaviorType.CODE_IMPLEMENTATION),
    DISASTER_RECOVERY("재해 복구", "Disaster Recovery", "災害復旧", OutputBehaviorType.CODE_IMPLEMENTATION),
    CLOUD_BACKUP("클라우드 백업", "Cloud Backup", "クラウドバックアップ", OutputBehaviorType.CODE_IMPLEMENTATION),
    LOAD_BALANCING("로드 밸런싱", "Load Balancing", "ロードバランシング", OutputBehaviorType.CODE_IMPLEMENTATION),
    AUTO_SCALING("자동 스케일링", "Auto Scaling", "自動スケーリング", OutputBehaviorType.CODE_IMPLEMENTATION),
    CLOUD_NETWORKING("클라우드 네트워킹", "Cloud Networking", "クラウドネットワーキング", OutputBehaviorType.CODE_IMPLEMENTATION);

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final OutputBehaviorType outputBehavior;

    @Override
    public String key() {
        return "ACTION.CLOUD_SERVICES." + name();
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

