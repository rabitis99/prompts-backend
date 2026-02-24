package org.example.sharedprompts.github.dto.config.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * GitHub Webhook 설정 생성 요청 DTO.
 * - repoFullName: GitHub repository full name (owner/repo)
 * - webhookSecret: (선택) GitHub Webhook 설정 시 입력한 Secret. X-Hub-Signature-256 검증에 사용. 입력 시 DB에 저장됩니다.
 *
 * promptId는 path(`/prompts/{promptId}/github/webhooks`) 에서 받고,
 * bodyPromptId는 서버에서 promptId로 관리합니다.
 */
public record GitHubWebhookCreateRequestDto(
        @NotBlank
        @JsonProperty("repo_full_name")
        String repoFullName,
        @JsonProperty("webhook_secret")
        String webhookSecret
) {
}
