package org.example.sharedprompts.module.domain.production.entity.job;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.global.entity.BaseEntity;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;

import java.time.Instant;

/**
 * AI 콘텐츠 생성을 위한 비동기 Job 엔티티
 * 
 * 책임:
 * - Job의 생명주기 관리 (생성 → 처리 → 완료/실패)
 * - 상태 전이 통제 (단순화된 상태 머신)
 * - 재시도 정책 (retryCount 증가 + FAILED → PENDING 전이만 허용)
 * 
 * 제거된 책임:
 * - 로그 저장 (rawResponse, parsedResponse 등은 별도 로그 엔티티로 분리 고려)
 * - 세부 단계 추적 (AI_CALLED, PARSED, RENDERED 등)
 */
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

    /**
     * 생성된 Artifact ID (연관관계로 변경 고려)
     */
    @Column(name = "artifact_id", length = 50)
    private String artifactId;

    /**
     * 재시도 횟수
     */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * 사용된 AI 모델 이름
     */
    @Column(name = "model_name", length = 100)
    private String modelName;

    /**
     * 토큰 사용량 (JSON 형태: {"promptTokens": 100, "completionTokens": 200, "totalTokens": 300})
     */
    @Column(name = "token_usage", columnDefinition = "TEXT")
    private String tokenUsage;

    /**
     * 사용된 Prompt 버전 (고정)
     */
    @Column(name = "prompt_version", length = 50)
    private String promptVersion;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    /* =========================
       State Transition Methods
       ========================= */

    /**
     * Job 처리 시작 (PENDING → PROCESSING)
     */
    public void start() {
        if (this.status != JobStatus.PENDING) {
            throw new IllegalStateException(
                String.format("Cannot start job: expected PENDING, but was %s", this.status)
            );
        }
        this.status = JobStatus.PROCESSING;
        this.startedAt = Instant.now();
    }

    /**
     * Job 성공 처리 (PROCESSING → SUCCESS)
     */
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

    /**
     * Job 실패 처리 (PROCESSING → FAILED)
     */
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

    /**
     * 재시도를 위한 상태 전이 (FAILED → PENDING)
     * 재시도 횟수도 함께 증가
     */
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

    /* =========================
       Query Methods
       ========================= */

    /**
     * 완료 여부 확인
     */
    public boolean isCompleted() {
        return status == JobStatus.SUCCEEDED;
    }

    /**
     * 실패 여부 확인
     */
    public boolean isFailed() {
        return status == JobStatus.FAILED;
    }

    /**
     * 최종 상태 여부 확인 (재처리 불가능)
     */
    public boolean isFinalState() {
        return status == JobStatus.SUCCEEDED || status == JobStatus.FAILED;
    }

    /**
     * 처리 중 여부 확인
     */
    public boolean isProcessing() {
        return status == JobStatus.PROCESSING;
    }

    /**
     * 대기 중 여부 확인
     */
    public boolean isPending() {
        return status == JobStatus.PENDING;
    }

    /* =========================
       Business Methods
       ========================= */

    /**
     * Artifact ID 설정
     */
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    /**
     * Prompt 버전 설정
     */
    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    /**
     * AI 모델 정보 설정
     */
    public void setModelInfo(String modelName, String tokenUsage) {
        this.modelName = modelName;
        this.tokenUsage = tokenUsage;
    }
}
