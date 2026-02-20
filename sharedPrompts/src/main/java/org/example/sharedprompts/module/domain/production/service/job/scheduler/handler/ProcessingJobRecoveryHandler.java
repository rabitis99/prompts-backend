package org.example.sharedprompts.module.domain.production.service.job.scheduler.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;
import org.example.sharedprompts.module.domain.production.service.job.process.JobProcessor;
import org.springframework.stereotype.Component;

/**
 * PROCESSING 상태 Job 복구 Handler
 * 단일 책임: PROCESSING 상태 Job의 복구 로직만 담당
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessingJobRecoveryHandler implements JobRecoveryHandler {
    
    private final JobProcessor jobProcessor;
    
    @Override
    public boolean supports(JobStatus status) {
        return status == JobStatus.PROCESSING;
    }
    
    @Override
    public void recover(JobEntity job) {
        log.info("Recovering PROCESSING job - jobId: {}, retryCount: {}", 
            job.getJobId(), job.getRetryCount());
        
        // 타임아웃된 PROCESSING 상태 Job의 비동기 재처리 시도
        // jobLockService.acquireJobLock()이 동시 처리를 방지하므로,
        // 원본 처리 스레드가 살아있으면 락 획득에 실패하여 조기 반환됩니다.
        // 따라서 중복 처리 위험 없이 안전하게 재처리를 시도할 수 있습니다.
        jobProcessor.processJobAsync(job.getJobId());
    }
}

