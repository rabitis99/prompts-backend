package org.example.sharedprompts.module.domain.production.repository.job;

import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Job Repository
 */
public interface JobRepository extends JpaRepository<JobEntity, Long> {

    /**
     * 멱등성 키로 Job 조회 (Lock 없이)
     * Insert-first 전략에서 중복 예외 발생 시 재조회용
     */
    Optional<JobEntity> findByIdempotencyKey(String idempotencyKey);

    /**
     * 멱등성 키로 Job 조회 (SELECT FOR UPDATE)
     * 상태 변경 전에 Lock을 획득하여 동시성 제어
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM JobEntity j WHERE j.idempotencyKey = :idempotencyKey")
    Optional<JobEntity> findByIdempotencyKeyForUpdate(@Param("idempotencyKey") String idempotencyKey);

    /**
     * Job ID로 조회
     */
    Optional<JobEntity> findByJobId(String jobId);

    /**
     * 사용자 ID로 Job 목록 조회
     */
    List<JobEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 장애 복구용: 일정 시간 이상 PROCESSING 상태인 Job 조회
     * 서버 장애로 인해 PROCESSING 상태로 남아있는 Job을 찾기 위함
     */
    @Query("""
        SELECT j FROM JobEntity j
        WHERE j.status = :status
        AND j.startedAt < :thresholdTime
        ORDER BY j.startedAt ASC
    """)
    List<JobEntity> findStuckJobs(
        @Param("status") JobStatus status,
        @Param("thresholdTime") Instant thresholdTime
    );

    @Query(value = """
        UPDATE production_jobs 
        SET status = 'PROCESSING', 
            started_at = :startedAt,
            version = version + 1
        WHERE job_id = :jobId 
          AND status = 'PENDING'
          AND version = :version
        """, nativeQuery = true)
    @Modifying
    @Transactional
    int acquireJobLock(
        @Param("jobId") String jobId, 
        @Param("startedAt") Instant startedAt,
        @Param("version") Long version
    );

    @Query("""
        SELECT j FROM JobEntity j
        WHERE j.status = :status
        AND j.startedAt < :thresholdTime
        ORDER BY j.startedAt ASC
    """)
    List<JobEntity> findByStatusAndStartedAtBefore(
        @Param("status") JobStatus status,
        @Param("thresholdTime") Instant thresholdTime
    );

    @Query("""
        SELECT j FROM JobEntity j
        WHERE j.status IN :statuses
        AND j.startedAt < :thresholdTime
        ORDER BY j.startedAt ASC
    """)
    List<JobEntity> findStuckJobsInStatuses(
        @Param("statuses") List<JobStatus> statuses,
        @Param("thresholdTime") Instant thresholdTime
    );

    List<JobEntity> findTop10ByStatusOrderByCreatedAtAsc(JobStatus status);
}

