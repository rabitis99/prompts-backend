package org.example.sharedprompts.github.dto.body.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Generated Markdown for both Issue and PR. Stored under the same jobId for DELIVERY.
 */
public record GitHubBodyResponseDto(
        @JsonProperty("job_id")
        String jobId,
        @JsonProperty("issue_body")
        String issueBody,
        @JsonProperty("pr_body")
        String prBody,
        @JsonProperty("stored_issue_file_key")
        String storedIssueFileKey,
        @JsonProperty("stored_pr_file_key")
        String storedPrFileKey
) {}
