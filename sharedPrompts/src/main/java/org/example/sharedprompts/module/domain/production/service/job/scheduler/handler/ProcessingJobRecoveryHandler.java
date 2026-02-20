package org.example.sharedprompts.module.domain.production.service.job.scheduler.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;
import org.example.sharedprompts.module.domain.production.repository.job.JobRepository;
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
    
    private final JobRepository jobRepository;
    private final JobProcessor jobProcessor;
    
    @Override
    public boolean supports(JobStatus status) {
        return status == JobStatus.PROCESSING;
    }
    
    @Override
    public void recover(JobEntity job) {
        // PROCESSING 상태는 FAILED로 표시 후 retry()를 통해 재처리
        // 또는 그냥 재처리 (PROCESSING 상태는 이미 처리 중이므로 재처리만)
        log.info("Recovering PROCESSING job - jobId: {}, retryCount: {}", 
            job.getJobId(), job.getRetryCount());
        
        // PROCESSING 상태는 이미 처리 중이므로, 그냥 재처리
        // (다른 스레드가 처리 중일 수 있으므로, 재처리만 시도)
        jobProcessor.processJobAsync(job.getJobId());
    }
}

