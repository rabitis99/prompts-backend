package org.example.sharedprompts.github.dto.config.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GitHub Webhook 설정 생성 응답 DTO.
 * - tenantKey: Webhook URL에 사용할 키
 * - webhookUrl: 완성된 Webhook URL (context-path 포함)
 */
public record GitHubWebhookConfigResponseDto(
        @JsonProperty("tenant_key")
        String tenantKey,
        @JsonProperty("webhook_url")
        String webhookUrl,
        @JsonProperty("repo_full_name")
        String repoFullName,
        @JsonProperty("body_prompt_id")
        Long bodyPromptId
) {
}
