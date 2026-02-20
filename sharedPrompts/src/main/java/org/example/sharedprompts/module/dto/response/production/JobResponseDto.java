package org.example.sharedprompts.module.dto.response.production;

import org.example.sharedprompts.module.domain.production.model.job.JobStatus;

import java.time.Instant;

/**
 * Job 응답 DTO
 */
public record JobResponseDto(
    String jobId,
    JobStatus status,
    String artifactId,
    String errorMessage,
    Instant createdAt,
    Instant completedAt
) {
}


