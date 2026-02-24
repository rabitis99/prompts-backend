package org.example.sharedprompts.module.github.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.service.PromptService;
import org.example.sharedprompts.module.github.domain.model.GitHubWebhookConfig;
import org.example.sharedprompts.module.github.port.in.CreateWebhookConfigUseCase;
import org.example.sharedprompts.module.github.port.out.WebhookConfigPersistencePort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * GitHub Webhook 설정 생성/조회 유스케이스 구현.
 *
 * 역할:
 * - Prompt 존재/권한 검증
 * - 기존 설정 조회 또는 신규 생성 (idempotent)
 * - TenantKey 생성 (보안)
 * - 동시 생성 처리 (DataIntegrityViolationException → retry)
 *
 * 마이그레이션:
 * - 기존 GitHubWebhookConfigApplicationService → 이 서비스로 통합
 * - Port 기반 영속성 호출
 *
 * @see CreateWebhookConfigUseCase
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreateWebhookConfigService implements CreateWebhookConfigUseCase {

  private final WebhookConfigPersistencePort configPort;
  private final PromptService promptService;
  private final CreateWebhookConfigNewTxHelper newTxHelper;

  private final SecureRandom secureRandom = new SecureRandom();

  @Transactional
  @Override
  public GitHubWebhookConfig createOrGet(Long ownerUserId, Long promptId, String repoFullName, String webhookSecret) {
    // Prompt 존재 및 권한 검증
    promptService.getPromptDetail(promptId, ownerUserId);

    return configPort.findByOwner(ownerUserId, repoFullName)
        .map(existing -> updateExisting(existing, promptId, webhookSecret))
        .orElseGet(() -> createNew(ownerUserId, promptId, repoFullName, webhookSecret));
  }

  private GitHubWebhookConfig updateExisting(GitHubWebhookConfig existing, Long promptId, String webhookSecret) {
    if (!existing.getBodyPromptId().equals(promptId)) {
      Long oldPromptId = existing.getBodyPromptId();
      existing.updateBodyPromptId(promptId);
      log.info("Updated bodyPromptId for webhook config - oldPromptId: {}, newPromptId: {}", oldPromptId, promptId);
    }
    if (webhookSecret != null && !webhookSecret.isBlank()) {
      existing.updateWebhookSecret(webhookSecret);
      log.debug("Updated webhook_secret for tenantKey: {}", existing.getTenantKey());
    }
    configPort.save(existing);
    return existing;
  }

  private GitHubWebhookConfig createNew(Long ownerUserId, Long promptId, String repoFullName, String webhookSecret) {
    String tenantKey = generateTenantKey();
    GitHubWebhookConfig config = GitHubWebhookConfig.builder()
        .tenantKey(tenantKey)
        .repoFullName(repoFullName)
        .ownerUserId(ownerUserId)
        .bodyPromptId(promptId)
        .enabled(true)
        .webhookSecret(webhookSecret != null && !webhookSecret.isBlank() ? webhookSecret : null)
        .build();

    try {
      configPort.save(config);
      log.info("Created GitHub webhook config - ownerUserId: {}, repo: {}, tenantKey: {}, promptId: {}",
          ownerUserId, repoFullName, tenantKey, promptId);
      return config;
    } catch (DataIntegrityViolationException e) {
      log.warn("Duplicate webhook config (concurrent create), re-fetching - ownerUserId: {}, repo: {}",
          ownerUserId, repoFullName);
      return newTxHelper.findExistingInNewTransaction(ownerUserId, repoFullName)
          .orElseThrow(() -> new IllegalStateException(
              "Unique constraint violated but config not found after conflict", e));
    }
  }

  private String generateTenantKey() {
    byte[] bytes = new byte[12]; // 96 bits
    secureRandom.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    return "ghw_" + token;
  }
}
