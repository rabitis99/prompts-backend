package org.example.sharedprompts.module.domain.production.entity.job;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.global.entity.BaseEntity;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * AI 콘텐츠 생성을 위한 비동기 Job 엔티티
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
     * AI가 생성한 원본 응답 (rawResponse) - 반드시 저장
     * 큰 데이터이므로 LONGTEXT 타입 사용
     */
    @Column(name = "raw_response", columnDefinition = "LONGTEXT")
    private String rawResponse;

    /**
     * 파싱된 응답 (parsedResponse) - JSON 형태
     */
    @Column(name = "parsed_response", columnDefinition = "LONGTEXT")
    private String parsedResponse;

    /**
     * AI가 생성한 최종 콘텐츠 (렌더링 전)
     * 큰 데이터이므로 LONGTEXT 타입 사용
     */
    @Column(name = "ai_generated_content", columnDefinition = "LONGTEXT")
    private String aiGeneratedContent;

    /**
     * 생성된 Artifact ID
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


    public static JobEntity create(
            Long promptId,
            Long userId,
            String commandType,
            String commandJson,
            String userInput,
            String idempotencyKey
    ) {
        return JobEntity.builder()
                .jobId(UUID.randomUUID().toString())
                .idempotencyKey(idempotencyKey)
                .promptId(promptId)
                .userId(userId)
                .commandType(commandType)
                .commandJson(commandJson)
                .userInput(userInput)
                .status(JobStatus.PENDING)
                .build();
    }

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
     * AI 호출 완료 (PROCESSING → AI_CALLED)
     */
    public void markAiCalled(String rawResponse, String modelName, String tokenUsage) {
        if (this.status != JobStatus.PROCESSING) {
            throw new IllegalStateException(
                String.format("Cannot mark AI called: expected PROCESSING, but was %s", this.status)
            );
        }
        this.status = JobStatus.AI_CALLED;
        this.rawResponse = rawResponse;
        this.modelName = modelName;
        this.tokenUsage = tokenUsage;
    }

    /**
     * 파싱 완료 (AI_CALLED → PARSED)
     */
    public void markParsed(String parsedResponse) {
        if (this.status != JobStatus.AI_CALLED) {
            throw new IllegalStateException(
                String.format("Cannot mark parsed: expected AI_CALLED, but was %s", this.status)
            );
        }
        this.status = JobStatus.PARSED;
        this.parsedResponse = parsedResponse;
    }

    /**
     * 파싱 실패 (AI_CALLED → PARSE_FAILED)
     */
    public void markParseFailed(String errorMessage) {
        if (this.status != JobStatus.AI_CALLED) {
            throw new IllegalStateException(
                String.format("Cannot mark parse failed: expected AI_CALLED, but was %s", this.status)
            );
        }
        this.status = JobStatus.PARSE_FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
    }

    /**
     * 렌더링 완료 (PARSED → RENDERED)
     */
    public void markRendered(String aiGeneratedContent) {
        if (this.status != JobStatus.PARSED) {
            throw new IllegalStateException(
                String.format("Cannot mark rendered: expected PARSED, but was %s", this.status)
            );
        }
        this.status = JobStatus.RENDERED;
        this.aiGeneratedContent = aiGeneratedContent;
    }

    /**
     * 파일 저장 완료 (RENDERED → STORED)
     */
    public void markStored(String artifactId) {
        if (this.status != JobStatus.RENDERED) {
            throw new IllegalStateException(
                String.format("Cannot mark stored: expected RENDERED, but was %s", this.status)
            );
        }
        this.status = JobStatus.STORED;
        this.artifactId = artifactId;
    }

    /**
     * Job 완료 처리 (STORED → COMPLETED)
     */
    public void complete() {
        if (this.status != JobStatus.STORED) {
            throw new IllegalStateException(
                String.format("Cannot complete job: expected STORED, but was %s", this.status)
            );
        }
        this.status = JobStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    /**
     * Job 실패 처리 (어떤 상태에서든 → FAILED)
     */
    public void fail(String errorMessage) {
        if (this.status == JobStatus.COMPLETED || this.status == JobStatus.FAILED || this.status == JobStatus.PARSE_FAILED) {
            throw new IllegalStateException(
                String.format("Cannot fail job: job is already in final state %s", this.status)
            );
        }
        this.status = JobStatus.FAILED;
        this.completedAt = Instant.now();
        this.errorMessage = errorMessage;
    }

    /**
     * 재시도 횟수 증가
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }

    /**
     * Artifact ID 설정
     */
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void resetToPending() {
        if (this.status != JobStatus.PROCESSING && this.status != JobStatus.AI_CALLED) {
            throw new IllegalStateException(
                String.format("Cannot reset job to PENDING: expected PROCESSING or AI_CALLED, but was %s", this.status)
            );
        }
        this.status = JobStatus.PENDING;
        this.startedAt = null;
    }

    public void resetToRetry() {
        if (this.status == JobStatus.COMPLETED || this.status == JobStatus.FAILED
                || this.status == JobStatus.PARSE_FAILED || this.status == JobStatus.PENDING) {
            throw new IllegalStateException(
                String.format("Cannot reset job to retry: job is in state %s", this.status)
            );
        }
        this.status = JobStatus.PENDING;
        this.startedAt = null;
        incrementRetryCount();
    }

    /**
     * 멱등성 키 재제출로 인한 실패 Job 재시도 (FAILED/PARSE_FAILED → PENDING)
     */
    public void resetForIdempotencyRetry() {
        if (this.status != JobStatus.FAILED && this.status != JobStatus.PARSE_FAILED) {
            throw new IllegalStateException(
                String.format("Cannot reset for retry: expected FAILED or PARSE_FAILED, but was %s", this.status)
            );
        }
        this.status = JobStatus.PENDING;
        this.startedAt = null;
        this.completedAt = null;
        this.errorMessage = null;
        this.rawResponse = null;
        this.parsedResponse = null;
        this.aiGeneratedContent = null;
        this.artifactId = null;
        incrementRetryCount();
    }

    /**
     * PARSE_FAILED → AI_CALLED 복원 (재파싱 허용)
     */
    public void resetToParsable() {
        if (this.status != JobStatus.PARSE_FAILED) {
            throw new IllegalStateException(
                String.format("Cannot reset to parsable: expected PARSE_FAILED, but was %s", this.status)
            );
        }
        this.status = JobStatus.AI_CALLED;
        this.errorMessage = null;
        this.completedAt = null;
    }

    /**
     * Prompt 버전 설정
     */
    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    /**
     * 완료 여부 확인
     */
    public boolean isCompleted() {
        return status == JobStatus.COMPLETED;
    }

    /**
     * 실패 여부 확인
     */
    public boolean isFailed() {
        return status == JobStatus.FAILED || status == JobStatus.PARSE_FAILED;
    }

    /**
     * 최종 상태 여부 확인 (재처리 불가능)
     */
    public boolean isFinalState() {
        return status == JobStatus.COMPLETED || status == JobStatus.FAILED || status == JobStatus.PARSE_FAILED;
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
}

