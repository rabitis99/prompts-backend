package org.example.sharedprompts.module.domain.production.entity.job;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.global.entity.BaseEntity;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;

import java.time.Instant;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
    name = "production_jobs",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_jobs_idempotency_key",
            columnNames = {"idempotency_key"}
        )
    },
    indexes = {
        @Index(name = "idx_jobs_user_id_created_at", columnList = "user_id, created_at"),
        @Index(name = "idx_jobs_status_started_at", columnList = "status, started_at"),
        @Index(name = "idx_jobs_idempotency_key", columnList = "idempotency_key")
    }
)
public class JobEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, unique = true, length = 36)
    private String jobId;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 255)
    private String idempotencyKey;

    @Column(name = "prompt_id", nullable = false)
    private Long promptId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    @Column(name = "command_type", nullable = false, length = 50)
    private String commandType;

    @Column(name = "command_json", nullable = false, columnDefinition = "TEXT")
    private String commandJson;

    @Column(name = "user_input", columnDefinition = "TEXT")
    private String userInput;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "artifact_id", length = 50)
    private String artifactId;

    /**
     * Production(Artifact) ID. Job 완료 시 생성된 production_artifacts.id와 동일하게 설정하여
     * N+1 없이 Job 상태 응답에 production_id를 포함할 수 있게 합니다.
     */
    @Column(name = "production_id")
    private Long productionId;

    /**
     * 재시도 횟수. 메시지 레벨 재시도(markAsRetrying)와 수동 복구(retry) 모두에서 증가합니다.
     * 한 실패 사이클에서 두 경로가 겹치지 않지만, 생명주기 전체로 보면 둘 다 반영된 총 시도 횟수입니다.
     */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "token_usage", columnDefinition = "TEXT")
    private String tokenUsage;

    @Column(name = "prompt_version", length = 50)
    private String promptVersion;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    public void start() {
        if (this.status != JobStatus.PENDING) {
            throw new BaseException(
                    ModuleErrorCode.JOB_INVALID_STATUS,
                    null,
                    String.format("Cannot start job: expected PENDING, but was %s", this.status)
            );
        }
        this.status = JobStatus.PROCESSING;
        this.startedAt = Instant.now();
    }

    public void complete(String artifactId) {
        if (this.status != JobStatus.PROCESSING) {
            throw new BaseException(
                    ModuleErrorCode.JOB_INVALID_STATUS,
                    null,
                    String.format("Cannot complete job: expected PROCESSING, but was %s", this.status)
            );
        }
        this.status = JobStatus.SUCCEEDED;
        this.artifactId = artifactId;
        this.productionId = parseProductionIdOrNull(artifactId);
        this.completedAt = Instant.now();
    }

    public void fail(String errorMessage) {
        if (this.status != JobStatus.PROCESSING) {
            throw new BaseException(
                    ModuleErrorCode.JOB_INVALID_STATUS,
                    null,
                    String.format("Cannot fail job: expected PROCESSING, but was %s", this.status)
            );
        }
        this.status = JobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
    }

    /**
     * P2-2: 수동 복구 시 FAILED → PENDING. retryCount 증가.
     * markAsRetrying()은 메시지 레벨 재시도 발행 시 호출되며, retry()는 복구 스케줄러 등에서 호출됩니다.
     */
    public void retry() {
        if (this.status != JobStatus.FAILED) {
            throw new BaseException(
                    ModuleErrorCode.JOB_INVALID_STATUS,
                    null,
                    String.format("Cannot retry job: expected FAILED, but was %s", this.status)
            );
        }
        this.status = JobStatus.PENDING;
        this.startedAt = null;
        this.completedAt = null;
        this.errorMessage = null;
        this.artifactId = null;
        this.retryCount++;
    }

    /**
     * P2-2: 메시지 레벨 retry 발행 성공 시 RETRYING 상태로 전이. FAILED → RETRYING.
     * State transition validation is performed by JobStateMachine before calling this.
     */
    public void markAsRetrying() {
        this.status = JobStatus.RETRYING;
        this.retryCount++;
    }

    /**
     * P2-2: RETRYING 상태의 job을 consumer가 소비할 때 PROCESSING으로 전이. RETRYING → PROCESSING.
     * State transition validation is performed by JobStateMachine before calling this.
     */
    public void startFromRetrying() {
        this.status = JobStatus.PROCESSING;
        this.startedAt = Instant.now();
        this.errorMessage = null;
        this.artifactId = null;
        this.completedAt = null;
    }

    /**
     * P3-1: AI/S3 호출 timeout 시 UNKNOWN 상태로 전이. PROCESSING → UNKNOWN.
     * 즉시 재호출 금지 - UnknownJobRecoveryScheduler가 5분 주기로 복구.
     * State transition validation is performed by JobStateMachine before calling this.
     */
    public void markAsUnknown(String reason) {
        this.status = JobStatus.UNKNOWN;
        this.errorMessage = reason;
    }

    /**
     * P3-1: 복구 스케줄러가 UNKNOWN → SUCCEEDED 전이.
     * State transition validation is performed by JobStateMachine before calling this.
     */
    public void recoverAsSucceeded(String artifactId) {
        this.status = JobStatus.SUCCEEDED;
        this.artifactId = artifactId;
        this.productionId = parseProductionIdOrNull(artifactId);
        this.completedAt = Instant.now();
        this.errorMessage = null;
    }

    /**
     * P3-1: 복구 스케줄러가 UNKNOWN → FAILED 전이.
     * State transition validation is performed by JobStateMachine before calling this.
     */
    public void recoverAsFailed(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
    }

    /**
     * 멱등성 키 중복(DataIntegrityViolationException) 발생 후, 기존 FAILED job을 재시도 가능한 상태로 되돌립니다.
     */
    public void resetForIdempotencyRetry() {
        retry();
    }

    public boolean isCompleted() {
        return status == JobStatus.SUCCEEDED;
    }

    public boolean isFailed() {
        return status == JobStatus.FAILED;
    }

    /**
     * P3-1: UNKNOWN은 isFinalState에 포함 — consumer가 재처리하지 않도록
     * UnknownJobRecoveryScheduler가 별도로 복구 처리
     */
    public boolean isFinalState() {
        return status == JobStatus.SUCCEEDED
                || status == JobStatus.FAILED
                || status == JobStatus.UNKNOWN;
    }

    public boolean isProcessing() {
        return status == JobStatus.PROCESSING;
    }

    public boolean isPending() {
        return status == JobStatus.PENDING;
    }

    public boolean isRetrying() {
        return status == JobStatus.RETRYING;
    }

    public boolean isUnknown() {
        return status == JobStatus.UNKNOWN;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
        this.productionId = parseProductionIdOrNull(artifactId);
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public void setModelInfo(String modelName, String tokenUsage) {
        this.modelName = modelName;
        this.tokenUsage = tokenUsage;
    }

    /**
     * Parses artifactId to Long when it is numeric (e.g. production_artifacts.id);
     * returns null for null, blank, or non-numeric values (e.g. UUID, test values).
     * Avoids NumberFormatException and supports VARCHAR(50) artifactId storage.
     */
    private static Long parseProductionIdOrNull(String artifactId) {
        if (artifactId == null || artifactId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(artifactId);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
