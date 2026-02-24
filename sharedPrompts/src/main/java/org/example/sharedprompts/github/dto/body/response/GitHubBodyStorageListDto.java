package org.example.sharedprompts.github.dto.body.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * GitHub 본문 저장 목록 API 응답 DTO.
 * 본문 내용은 포함하지 않고 메타데이터만 반환합니다.
 */
public record GitHubBodyStorageListDto(
        @JsonProperty("id")
        Long id,
        @JsonProperty("tenant_key")
        String tenantKey,
        @JsonProperty("repo_full_name")
        String repoFullName,
        @JsonProperty("job_id")
        String jobId,
        @JsonProperty("event_type")
        String eventType,
        @JsonProperty("created_at")
        Instant createdAt
) {}
