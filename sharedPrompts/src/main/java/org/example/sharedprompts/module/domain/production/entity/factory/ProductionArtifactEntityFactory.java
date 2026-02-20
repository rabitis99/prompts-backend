package org.example.sharedprompts.module.domain.production.entity.factory;

import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactEntity;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;

import java.util.ArrayList;

/**
 * ProductionArtifactEntity Factory
 * ProductionArtifactEntity 생성 로직을 담당합니다.
 */
public final class ProductionArtifactEntityFactory {

    private ProductionArtifactEntityFactory() {
    }

    /**
     * Production Artifact 생성
     * Job 성공 시 생성되는 결과 의미 단위
     */
    public static ProductionArtifactEntity create(
            Long jobId,
            String tenantId,
            Long userId,
            ProductionCommandType commandType
    ) {
        return ProductionArtifactEntity.builder()
                .jobId(jobId)
                .tenantId(tenantId)
                .userId(userId)
                .commandType(commandType)
                .artifacts(new ArrayList<>())
                .build();
    }
}

