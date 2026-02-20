package org.example.sharedprompts.module.domain.production.entity.job;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.global.entity.BaseEntity;

import java.time.Instant;

/**
 * Job 실패 이력 엔티티
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
    name = "production_job_failure_logs",
    indexes = {
        @Index(name = "idx_job_failure_logs_job_id", columnList = "job_id"),
        @Index(name = "idx_job_failure_logs_failed_at", columnList = "failed_at"),
        // 복합 인덱스: WHERE job_id = ? ORDER BY failed_at DESC 쿼리 최적화
        // 실패 로그가 대량 축적될 경우 쿼리 성능 향상에 효과적
        @Index(name = "idx_job_failure_logs_job_id_failed_at", columnList = "job_id, failed_at")
    }
)
public class JobFailureLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "raw_response", columnDefinition = "LONGTEXT")
    private String rawResponse;

    @Column(name = "stack_trace", columnDefinition = "LONGTEXT")
    private String stackTrace;

    @Column(name = "failed_at", nullable = false)
    private Instant failedAt;

}

