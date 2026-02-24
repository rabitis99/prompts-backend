package org.example.sharedprompts.module.github.port.out;

import org.example.sharedprompts.module.github.domain.model.GitHubWebhookConfig;

import java.util.Optional;

/**
 * Webhook 설정 영속성 포트.
 *
 * 구현 책임:
 * - {@link org.example.sharedprompts.module.github.adapter.out.persistence.JpaWebhookConfigAdapter}
 *
 * LSP 계약:
 * - 조회: Optional로 반환 (없으면 empty)
 * - 저장: 예외 발생 (DataIntegrityViolationException 등)
 */
public interface WebhookConfigPersistencePort {

  /**
   * tenantKey + repoFullName로 활성화된 설정 조회.
   *
   * @param tenantKey webhook 테넌트 식별자
   * @param repoFullName GitHub 저장소 (owner/repo)
   * @return 설정 또는 empty
   * @throws PersistenceException DB 조회 실패
   */
  Optional<GitHubWebhookConfig> findByTenantKey(String tenantKey, String repoFullName);

  /**
   * ownerUserId + repoFullName로 활성화된 설정 조회.
   *
   * @param ownerUserId 사용자 ID
   * @param repoFullName GitHub 저장소
   * @return 설정 또는 empty
   * @throws PersistenceException DB 조회 실패
   */
  Optional<GitHubWebhookConfig> findByOwner(Long ownerUserId, String repoFullName);

  /**
   * Webhook 설정 저장/업데이트.
   *
   * @param config 설정 엔티티
   * @throws IllegalArgumentException config null
   * @throws PersistenceException DB 저장 실패 (제약 위배 포함)
   */
  void save(GitHubWebhookConfig config);
}
