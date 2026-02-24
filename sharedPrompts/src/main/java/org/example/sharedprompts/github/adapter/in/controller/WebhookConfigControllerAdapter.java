package org.example.sharedprompts.github.adapter.in.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.github.domain.model.GitHubWebhookConfig;
import org.example.sharedprompts.github.dto.config.request.GitHubWebhookCreateRequestDto;
import org.example.sharedprompts.github.dto.config.response.GitHubWebhookConfigResponseDto;
import org.example.sharedprompts.github.port.in.CreateWebhookConfigUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * GitHub Webhook 설정 관리 input adapter.
 *
 * 책임:
 * - HTTP 요청 수신
 * - Webhook URL 생성
 * - CreateWebhookConfigUseCase (Port) 호출
 * - HTTP 응답 반환
 *
 * 변경점:
 * - GitHubWebhookConfigApplicationService → CreateWebhookConfigUseCase (Port)
 * - 모든 비즈니스 로직은 Port 구현체에서 담당
 *
 * 스펙 유지:
 * - 경로: /prompts/{promptId}/github/webhooks (POST)
 * - 요청: GitHubWebhookCreateRequestDto (repoFullName, webhookSecret)
 * - 응답: GitHubWebhookConfigResponseDto (tenantKey, webhookUrl, repoFullName, bodyPromptId)
 */
@RestController
@RequestMapping("/prompts/{promptId}/github/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookConfigControllerAdapter {

  private final CreateWebhookConfigUseCase createWebhookConfigUseCase;

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

    GitHubWebhookConfig config = createWebhookConfigUseCase.createOrGet(
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
