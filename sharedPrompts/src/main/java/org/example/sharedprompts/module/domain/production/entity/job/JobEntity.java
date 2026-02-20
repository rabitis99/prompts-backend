package org.example.sharedprompts.module.domain.production.entity.job;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.global.entity.BaseEntity;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;

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
            throw new IllegalStateException(
                String.format("Cannot start job: expected PENDING, but was %s", this.status)
            );
        }
        this.status = JobStatus.PROCESSING;
        this.startedAt = Instant.now();
    }

    public void complete(String artifactId) {
        if (this.status != JobStatus.PROCESSING) {
            throw new IllegalStateException(
                String.format("Cannot complete job: expected PROCESSING, but was %s", this.status)
            );
        }
        this.status = JobStatus.SUCCEEDED;
        this.artifactId = artifactId;
        this.completedAt = Instant.now();
    }

    public void fail(String errorMessage) {
        if (this.status != JobStatus.PROCESSING) {
            throw new IllegalStateException(
                String.format("Cannot fail job: expected PROCESSING, but was %s", this.status)
            );
        }
        this.status = JobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
    }

    public void retry() {
        if (this.status != JobStatus.FAILED) {
            throw new IllegalStateException(
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

    public boolean isCompleted() {
        return status == JobStatus.SUCCEEDED;
    }

    public boolean isFailed() {
        return status == JobStatus.FAILED;
    }

    public boolean isFinalState() {
        return status == JobStatus.SUCCEEDED || status == JobStatus.FAILED;
    }

    public boolean isProcessing() {
        return status == JobStatus.PROCESSING;
    }

    public boolean isPending() {
        return status == JobStatus.PENDING;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public void setModelInfo(String modelName, String tokenUsage) {
        this.modelName = modelName;
        this.tokenUsage = tokenUsage;
    }
}
