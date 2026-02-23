package org.example.sharedprompts.module.dto.request.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * GitHub Webhook 설정 생성 요청 DTO.
 * - repoFullName: GitHub repository full name (owner/repo)
 *
 * promptId는 path(`/prompts/{promptId}/github/webhooks`) 에서 받고,
 * bodyPromptId는 서버에서 promptId로 관리합니다.
 */
public record GitHubWebhookCreateRequestDto(
        @NotBlank
        @JsonProperty("repo_full_name")
        String repoFullName
) {
}

