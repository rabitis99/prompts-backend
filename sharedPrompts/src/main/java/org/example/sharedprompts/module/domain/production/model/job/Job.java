package org.example.sharedprompts.module.domain.production.model.job;

import lombok.Builder;
import lombok.Getter;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;

import java.time.Instant;
import java.util.UUID;

/**
 * AI 콘텐츠 생성을 위한 비동기 Job 모델
 */
@Getter
@Builder
public class Job {
    private final String jobId;
    private final Long promptId;
    private final Long userId;
    private final ProductionCommand command;
    private final String userInput;
    private JobStatus status;
    private String errorMessage;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private String aiGeneratedContent; // AI가 생성한 원본 콘텐츠 (Text/Image)
    private String artifactId; // 생성된 Artifact ID
    private Long productionId; // 생성된 Production(Artifact) ID (N+1 방지용으로 Job에 보관)

    public static Job create(Long promptId, Long userId, ProductionCommand command, String userInput) {
        return Job.builder()
                .jobId(UUID.randomUUID().toString())
                .promptId(promptId)
                .userId(userId)
                .command(command)
                .userInput(userInput)
                .status(JobStatus.PENDING)
                .createdAt(Instant.now())
                .build();
    }

    public void start() {
        this.status = JobStatus.PROCESSING;
        this.startedAt = Instant.now();
    }

    public void complete(String aiGeneratedContent) {
        this.status = JobStatus.SUCCEEDED;
        this.completedAt = Instant.now();
        this.aiGeneratedContent = aiGeneratedContent;
    }

    public void fail(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.completedAt = Instant.now();
        this.errorMessage = errorMessage;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public boolean isCompleted() {
        return status == JobStatus.SUCCEEDED;
    }

    public boolean isFailed() {
        return status == JobStatus.FAILED;
    }
}

