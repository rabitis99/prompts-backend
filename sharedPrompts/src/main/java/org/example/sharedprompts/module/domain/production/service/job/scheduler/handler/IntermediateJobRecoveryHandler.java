package org.example.sharedprompts.module.domain.production.service.job.scheduler.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;
import org.example.sharedprompts.module.domain.production.repository.job.JobRepository;
import org.example.sharedprompts.module.domain.production.service.job.process.JobProcessor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 중간 상태 (AI_CALLED, PARSED, RENDERED, STORED) Job 복구 Handler
 * 단일 책임: 중간 상태 Job의 복구 로직만 담당
 * 
 * 이미 저장된 데이터를 재활용하여 불필요한 AI 재호출을 방지
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IntermediateJobRecoveryHandler implements JobRecoveryHandler {
    
    private final JobRepository jobRepository;
    private final JobProcessor jobProcessor;
    
    private static final List<JobStatus> SUPPORTED_STATUSES = List.of(
        JobStatus.AI_CALLED,
        JobStatus.PARSED,
        JobStatus.RENDERED,
        JobStatus.STORED
    );
    
    @Override
    public boolean supports(JobStatus status) {
        return SUPPORTED_STATUSES.contains(status);
    }
    
    @Override
    public void recover(JobEntity job) {
        log.info("Recovering job from intermediate state - jobId: {}, status: {}, retryCount: {}", 
            job.getJobId(), job.getStatus(), job.getRetryCount());
        
        // AI_CALLED, PARSED, RENDERED, STORED 상태는 상태별 복구 메서드 사용
        // (이미 저장된 데이터를 재활용하여 불필요한 AI 재호출 방지)
        job.incrementRetryCount();
        jobRepository.save(job);
        
        jobProcessor.recoverJob(job.getJobId());
    }
}

