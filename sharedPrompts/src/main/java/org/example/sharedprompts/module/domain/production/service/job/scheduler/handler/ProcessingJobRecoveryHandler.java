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
        // PROCESSING 상태는 PENDING으로 리셋 후 재처리 (AI 호출 전이므로 전체 재시작)
        job.resetToRetry();
        jobRepository.save(job);
        
        log.info("Reset PROCESSING job to retry - jobId: {}, retryCount: {}", 
            job.getJobId(), job.getRetryCount());
        
        jobProcessor.processJobAsync(job.getJobId());
    }
}

