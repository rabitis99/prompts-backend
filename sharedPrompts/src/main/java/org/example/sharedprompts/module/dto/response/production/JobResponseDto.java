package org.example.sharedprompts.module.dto.response.production;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;

import java.time.Instant;

/**
 * Job 응답 DTO
 */
public record JobResponseDto(
    @JsonProperty("job_id")
    String jobId,
    @JsonProperty("status")
    JobStatus status,
    @JsonProperty("artifact_id")
    String artifactId,
    @JsonProperty("production_id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Long productionId,
    @JsonProperty("error_message")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String errorMessage,
    @JsonProperty("created_at")
    Instant createdAt,
    @JsonProperty("completed_at")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Instant completedAt
) {
}


