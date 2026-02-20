package org.example.sharedprompts.module.domain.production.entity.factory;

import org.example.sharedprompts.module.domain.production.entity.job.JobFailureLog;

import java.time.Instant;

/**
 * JobFailureLog Factory
 * JobFailureLog 생성 로직을 담당합니다.
 */
public final class JobFailureLogFactory {

    private JobFailureLogFactory() {
    }

    /**
     * JobFailureLog 생성
     */
    public static JobFailureLog create(
            Long jobId,
            String errorMessage,
            String rawResponse,
            String stackTrace
    ) {
        return JobFailureLog.builder()
                .jobId(jobId)
                .errorMessage(errorMessage)
                .rawResponse(rawResponse)
                .stackTrace(stackTrace)
                .failedAt(Instant.now())
                .build();
    }
}

