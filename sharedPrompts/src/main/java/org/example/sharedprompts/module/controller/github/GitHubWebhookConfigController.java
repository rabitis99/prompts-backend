package org.example.sharedprompts.module.controller.github;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.module.domain.github.GitHubWebhookConfigApplicationService;
import org.example.sharedprompts.module.domain.github.entity.GitHubWebhookConfig;
import org.example.sharedprompts.module.dto.request.github.GitHubWebhookCreateRequestDto;
import org.example.sharedprompts.module.dto.response.github.GitHubWebhookConfigResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * GitHub Webhook 설정 관리 API.
 * 인증된 사용자가 promptId(path) + repoFullName(body)를 넘기면
 * tenantKey를 생성/재사용하고 Webhook URL을 반환한다.
 */
@RestController
@RequestMapping("/prompts/{promptId}/github/webhooks")
@RequiredArgsConstructor
@Slf4j
public class GitHubWebhookConfigController {

    private final GitHubWebhookConfigApplicationService webhookConfigApplicationService;

    @PostMapping
    public ResponseEntity<CustomResponse<GitHubWebhookConfigResponseDto>> createOrGetConfig(
            @Valid @RequestBody GitHubWebhookCreateRequestDto request,
            @CurrentUser AuthUser authUser,
            @PathVariable Long promptId,
            HttpServletRequest httpServletRequest
    ) {
        Long userId = authUser.getId();
        log.info("GitHub webhook config create requested - userId: {}, promptId: {}, repo: {}",
                userId, promptId, request.repoFullName());

        GitHubWebhookConfig config = webhookConfigApplicationService.createOrGet(
                userId,
                promptId,
                request.repoFullName(),
                request.webhookSecret()
        );

        String webhookUrl = ServletUriComponentsBuilder
                .fromContextPath(httpServletRequest)
                .path("/webhooks/github/")
                .path(config.getTenantKey())
                .build()
                .toUriString();

        GitHubWebhookConfigResponseDto responseDto = new GitHubWebhookConfigResponseDto(
                config.getTenantKey(),
                webhookUrl,
                config.getRepoFullName(),
                config.getBodyPromptId()
        );

        return CustomResponseHelper.ok(responseDto);
    }
}

