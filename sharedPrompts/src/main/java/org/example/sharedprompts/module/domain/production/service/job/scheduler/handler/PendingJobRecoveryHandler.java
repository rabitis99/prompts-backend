package org.example.sharedprompts.module.domain.production.service.job.scheduler.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;
import org.example.sharedprompts.module.domain.production.repository.job.JobRepository;
import org.example.sharedprompts.module.domain.production.service.job.process.JobProcessor;
import org.springframework.stereotype.Component;

/**
 * PENDING 상태 Job 복구 Handler
 * 단일 책임: PENDING 상태 Job의 복구 로직만 담당
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PendingJobRecoveryHandler implements JobRecoveryHandler {
    
    private final JobRepository jobRepository;
    private final JobProcessor jobProcessor;
    
    @Override
    public boolean supports(JobStatus status) {
        return status == JobStatus.PENDING;
    }
    
    @Override
    public void recover(JobEntity job) {
        log.info("Retrying PENDING job - jobId: {}, retryCount: {}", 
            job.getJobId(), job.getRetryCount());
        
        // PENDING 상태인 경우: @Async 호출이 실패했을 가능성이 높으므로 재처리
        job.incrementRetryCount();
        jobRepository.save(job);
        
        // 비동기로 재처리 시작
        jobProcessor.processJobAsync(job.getJobId());
    }
}

