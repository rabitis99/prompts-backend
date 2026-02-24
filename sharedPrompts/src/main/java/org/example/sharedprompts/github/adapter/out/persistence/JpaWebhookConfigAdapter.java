package org.example.sharedprompts.github.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.github.domain.model.GitHubWebhookConfig;
import org.example.sharedprompts.github.domain.repository.GitHubWebhookConfigRepository;
import org.example.sharedprompts.github.port.exception.PersistenceException;
import org.example.sharedprompts.github.port.out.WebhookConfigPersistencePort;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * JPA 기반 WebhookConfig 영속성 포트 구현.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JpaWebhookConfigAdapter implements WebhookConfigPersistencePort {

  private final GitHubWebhookConfigRepository repository;

  @Override
  public Optional<GitHubWebhookConfig> findByTenantKey(String tenantKey, String repoFullName) {
    if (tenantKey == null || repoFullName == null) {
      return Optional.empty();
    }
    try {
      return repository.findByTenantKeyAndRepoFullNameAndEnabledTrue(tenantKey, repoFullName);
    } catch (Exception e) {
      log.warn("Failed to find webhook config - tenantKey: {}, repo: {}: {}", tenantKey, repoFullName, e.getMessage());
      throw new PersistenceException("DB query failed", e);
    }
  }

  @Override
  public Optional<GitHubWebhookConfig> findByOwner(Long ownerUserId, String repoFullName) {
    if (ownerUserId == null || repoFullName == null) {
      return Optional.empty();
    }
    try {
      return repository.findByOwnerUserIdAndRepoFullNameAndEnabledTrue(ownerUserId, repoFullName);
    } catch (Exception e) {
      log.warn("Failed to find webhook config - ownerUserId: {}, repo: {}: {}", ownerUserId, repoFullName, e.getMessage());
      throw new PersistenceException("DB query failed", e);
    }
  }

  @Override
  public void save(GitHubWebhookConfig config) {
    if (config == null) {
      throw new IllegalArgumentException("config cannot be null");
    }
    try {
      repository.save(config);
      log.debug("Webhook config saved - tenantKey: {}, repo: {}", config.getTenantKey(), config.getRepoFullName());
    } catch (Exception e) {
      log.error("Failed to save webhook config: {}", e.getMessage());
      throw new PersistenceException("DB save failed", e);
    }
  }
}
