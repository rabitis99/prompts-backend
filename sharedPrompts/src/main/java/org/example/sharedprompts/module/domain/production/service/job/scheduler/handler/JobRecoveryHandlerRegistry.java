package org.example.sharedprompts.module.domain.production.service.job.scheduler.handler;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Job 상태별 Handler 조회를 담당하는 Registry
 * 단일 책임: Handler 조회만 담당
 */
@Component
@RequiredArgsConstructor
public class JobRecoveryHandlerRegistry {
    
    private final List<JobRecoveryHandler> handlers;
    
    /**
     * JobStatus에 해당하는 Handler 조회
     * 
     * @param status Job 상태
     * @return 해당 상태를 처리하는 Handler
     * @throws IllegalArgumentException 지원하지 않는 상태인 경우
     */
    public JobRecoveryHandler getHandler(JobStatus status) {
        return handlers.stream()
            .filter(handler -> handler.supports(status))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "No handler found for status: " + status
            ));
    }
}

