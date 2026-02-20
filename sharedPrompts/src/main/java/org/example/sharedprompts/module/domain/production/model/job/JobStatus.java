package org.example.sharedprompts.module.domain.production.model.job;

/**
 * Job 처리 상태
 * 
 * 설계 원칙:
 * - 프로세스와 결과는 분리한다
 * - 상태는 Job만 가진다
 * - 세부 단계 추적 제거 (중간 상태 없음)
 */
public enum JobStatus {
    /**
     * 대기 중 (JobQueue 등록 완료)
     */
    PENDING,
    
    /**
     * 처리 중 (Worker가 Job 획득하여 처리 중)
     */
    PROCESSING,
    
    /**
     * 성공 (Job이 성공적으로 완료됨)
     */
    SUCCEEDED,
    
    /**
     * 실패 (처리 중 오류 발생, 재시도 가능)
     */
    FAILED
}

