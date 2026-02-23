package org.example.sharedprompts.module.dto.response.github;

/**
 * Generated Markdown for both Issue and PR. Stored under the same jobId for DELIVERY.
 */
public record GitHubBodyResponseDto(
        String jobId,
        String issueBody,
        String prBody,
        String storedIssueFileKey,
        String storedPrFileKey
) {}
