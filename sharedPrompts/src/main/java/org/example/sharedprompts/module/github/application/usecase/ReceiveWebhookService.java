package org.example.sharedprompts.module.github.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.github.adapter.out.webhook.WebhookPayloadHandlerRegistry;
import org.example.sharedprompts.module.github.domain.model.BodyGenerationRequest;
import org.example.sharedprompts.module.github.dto.body.response.GitHubBodyResponseDto;
import org.example.sharedprompts.module.github.port.in.ReceiveWebhookUseCase;
import org.example.sharedprompts.module.github.port.in.GenerateGitHubBodyUseCase;
import org.example.sharedprompts.module.github.dto.body.request.GitHubBodyRequestDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * GitHub webhook 수신 및 본문 생성 유스케이스 구현.
 *
 * 역할:
 * - Webhook 페이로드 파싱 (registry 패턴으로 이벤트별 핸들러 기동)
 * - 본문 생성 요청 생성
 * - GenerateGitHubBodyUseCase 호출로 본문 생성 및 저장
 *
 * @see ReceiveWebhookUseCase
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiveWebhookService implements ReceiveWebhookUseCase {

  private final WebhookPayloadHandlerRegistry payloadHandlerRegistry;
  private final GenerateGitHubBodyUseCase generateBodyUseCase;

  @Transactional
  @Override
  public Optional<GitHubBodyResponseDto> handlePush(String tenantKey, String deliveryId, String payload) {
    return parseAndGenerate("push", tenantKey, deliveryId, payload);
  }

  @Transactional
  @Override
  public Optional<GitHubBodyResponseDto> handlePullRequest(String tenantKey, String deliveryId, String payload) {
    return parseAndGenerate("pull_request", tenantKey, deliveryId, payload);
  }

  private Optional<GitHubBodyResponseDto> parseAndGenerate(String eventType, String tenantKey,
                                                            String deliveryId, String payload) {
    Optional<BodyGenerationRequest> parseResult = payloadHandlerRegistry.parse(eventType, payload);
    if (parseResult.isEmpty()) {
      log.debug("Failed to parse webhook payload for event: {}", eventType);
      return Optional.empty();
    }

    BodyGenerationRequest request = parseResult.get();
    String repoFullName = request.repoFullName();
    if (repoFullName == null || repoFullName.isBlank()) {
      log.debug("Parsed request missing repo");
      return Optional.empty();
    }

    GitHubBodyRequestDto dto = new GitHubBodyRequestDto(
        request.jobId(),
        deliveryId != null ? deliveryId : request.deliveryId(),
        request.sha(),
        repoFullName,
        request.branch(),
        request.baseBranch(),
        request.title(),
        request.author(),
        request.date(),
        request.commits(),
        request.files(),
        tenantKey
    );

    // TODO: bodyPromptId를 어디서 가져올지 결정 필요 (webhook config에서? 요청에서?)
    // 현재는 null로 설정하면 기본 템플릿 사용
    Long bodyPromptId = null;
    Long ownerUserId = null; // TODO: tenantKey로부터 조회 필요

    try {
      return Optional.of(generateBodyUseCase.generateWithPersistence(
          bodyPromptId, dto, tenantKey, eventType, ownerUserId));
    } catch (Exception e) {
      log.error("Body generation failed for {}: {}", eventType, e.getMessage());
      return Optional.empty();
    }
  }
}
