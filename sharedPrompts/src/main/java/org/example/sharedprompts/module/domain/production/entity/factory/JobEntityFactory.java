package org.example.sharedprompts.module.domain.production.entity.factory;

import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;

import java.util.UUID;

/**
 * JobEntity Factory
 * JobEntity 생성 로직을 담당합니다.
 */
public final class JobEntityFactory {

    private JobEntityFactory() {
    }

    /**
     * JobEntity 생성
     */
    public static JobEntity create(
            Long promptId,
            Long userId,
            String commandType,
            String commandJson,
            String userInput,
            String idempotencyKey,
            String tenantId
    ) {
        return JobEntity.builder()
                .jobId(UUID.randomUUID().toString())
                .idempotencyKey(idempotencyKey)
                .promptId(promptId)
                .userId(userId)
                .tenantId(tenantId)
                .commandType(commandType)
                .commandJson(commandJson)
                .userInput(userInput)
                .status(JobStatus.PENDING)
                .retryCount(0)
                .build();
    }
}

