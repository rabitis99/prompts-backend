package org.example.sharedprompts.module.domain.github;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.github.entity.GitHubWebhookConfig;
import org.example.sharedprompts.module.domain.github.repository.GitHubWebhookConfigRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * (tenant_key, repo_full_name)으로 X-Hub-Signature-256 검증에 쓸 시크릿을 DB에서 조회합니다.
 * findByTenantKeyAndRepoFullNameAndEnabledTrue만 사용합니다.
 */
@Component
@RequiredArgsConstructor
public class GitHubWebhookSecretResolver {

    private final GitHubWebhookConfigRepository webhookConfigRepository;

    /**
     * tenantKey + repoFullName에 해당하는 webhook_secret을 DB에서 반환합니다.
     * 없거나 null/blank면 빈 Optional (이 경우 검증은 수행하지 않음).
     */
    public Optional<String> resolveSecret(String tenantKey, String repoFullName) {
        if (tenantKey == null || tenantKey.isBlank() || repoFullName == null || repoFullName.isBlank()) {
            return Optional.empty();
        }
        return webhookConfigRepository.findByTenantKeyAndRepoFullNameAndEnabledTrue(tenantKey, repoFullName)
                .map(GitHubWebhookConfig::getWebhookSecret)
                .filter(s -> s != null && !s.isBlank());
    }
}
