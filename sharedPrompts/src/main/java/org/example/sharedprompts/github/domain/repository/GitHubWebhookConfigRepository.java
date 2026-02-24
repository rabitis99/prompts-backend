package org.example.sharedprompts.github.domain.repository;

import org.example.sharedprompts.github.domain.model.GitHubWebhookConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GitHubWebhookConfigRepository extends JpaRepository<GitHubWebhookConfig, Long> {

    Optional<GitHubWebhookConfig> findByTenantKeyAndRepoFullNameAndEnabledTrue(String tenantKey, String repoFullName);

    Optional<GitHubWebhookConfig> findByOwnerUserIdAndRepoFullNameAndEnabledTrue(Long ownerUserId, String repoFullName);
}
