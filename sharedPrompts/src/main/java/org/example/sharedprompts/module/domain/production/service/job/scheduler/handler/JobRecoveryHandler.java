package org.example.sharedprompts.module.domain.production.service.job.scheduler.handler;

import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;

/**
 * Job 상태별 복구 로직을 정의하는 인터페이스
 * Strategy Pattern을 사용하여 상태별 복구 전략을 분리
 */
public interface JobRecoveryHandler {
    
    /**
     * 해당 Handler가 지원하는 JobStatus인지 확인
     * 
     * @param status Job 상태
     * @return 지원 여부
     */
    boolean supports(JobStatus status);
    
    /**
     * Job 복구 수행
     * 
     * @param job 복구할 Job 엔티티
     */
    void recover(JobEntity job);
}

