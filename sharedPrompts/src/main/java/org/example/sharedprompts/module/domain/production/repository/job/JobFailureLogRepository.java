package org.example.sharedprompts.module.domain.production.repository.job;

import org.example.sharedprompts.module.domain.production.entity.job.JobFailureLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * JobFailureLog Repository
 */
public interface JobFailureLogRepository extends JpaRepository<JobFailureLog, Long> {

    /**
     * Job ID로 최신 실패 로그 조회
     */
    @Query("SELECT f FROM JobFailureLog f WHERE f.jobId = :jobId ORDER BY f.failedAt DESC")
    List<JobFailureLog> findByJobIdOrderByFailedAtDesc(@Param("jobId") Long jobId);

    /**
     * Job ID로 최신 실패 로그 1개 조회
     * Spring Data JPA 메서드 네이밍 규칙을 사용하여 LIMIT 1이 자동 적용됩니다.
     */
    Optional<JobFailureLog> findFirstByJobIdOrderByFailedAtDesc(Long jobId);
}

