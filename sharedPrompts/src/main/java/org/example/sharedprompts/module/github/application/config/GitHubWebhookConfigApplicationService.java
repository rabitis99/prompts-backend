package org.example.sharedprompts.module.github.application.config;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.service.PromptService;
import org.example.sharedprompts.module.github.domain.model.GitHubWebhookConfig;
import org.example.sharedprompts.module.github.domain.repository.GitHubWebhookConfigRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * GitHub Webhook 설정 관리 애플리케이션 서비스.
 * 인증된 사용자가 promptId + repo 를 넘기면 tenantKey를 생성/재사용하고 Webhook URL 구성을 돕습니다.
 */
@Service
@Slf4j
public class GitHubWebhookConfigApplicationService {

    private final GitHubWebhookConfigRepository webhookConfigRepository;
    private final PromptService promptService;

    @Lazy
    private GitHubWebhookConfigApplicationService self;

    private final SecureRandom secureRandom = new SecureRandom();

    public GitHubWebhookConfigApplicationService(
            GitHubWebhookConfigRepository webhookConfigRepository,
            PromptService promptService,
            @Lazy GitHubWebhookConfigApplicationService self) {
        this.webhookConfigRepository = webhookConfigRepository;
        this.promptService = promptService;
        this.self = self;
    }

    /**
     * ownerUserId + repoFullName 기준으로 설정을 찾고, 없으면 새로 생성해 반환.
     * bodyPromptId는 path의 promptId를 그대로 사용하며, 항상 유효한 prompt인지 검증한다.
     * webhookSecret이 넘어오면 DB에 저장하여 해당 tenant의 X-Hub-Signature-256 검증에 사용합니다.
     */
    @Transactional
    public GitHubWebhookConfig createOrGet(Long ownerUserId, Long promptId, String repoFullName, String webhookSecret) {
        // prompt 존재/권한 검증 (조회 가능해야 설정 생성 허용)
        promptService.getPromptDetail(promptId, ownerUserId);

        return webhookConfigRepository
                .findByOwnerUserIdAndRepoFullNameAndEnabledTrue(ownerUserId, repoFullName)
                .map(existing -> {
                    if (!existing.getBodyPromptId().equals(promptId)) {
                        Long oldPromptId = existing.getBodyPromptId();
                        existing.updateBodyPromptId(promptId);
                        log.info("Updated bodyPromptId for webhook config - ownerUserId: {}, repo: {}, oldPromptId: {}, newPromptId: {}",
                                ownerUserId, repoFullName, oldPromptId, promptId);
                    }
                    if (webhookSecret != null && !webhookSecret.isBlank()) {
                        existing.updateWebhookSecret(webhookSecret);
                        log.debug("Updated webhook_secret for tenantKey: {}", existing.getTenantKey());
                    }
                    return webhookConfigRepository.save(existing);
                })
                .orElseGet(() -> {
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
                        GitHubWebhookConfig saved = webhookConfigRepository.save(config);
                        log.info("Created GitHub webhook config - ownerUserId: {}, repo: {}, tenantKey: {}, promptId: {}",
                                ownerUserId, repoFullName, tenantKey, promptId);
                        return saved;
                    } catch (DataIntegrityViolationException e) {
                        log.warn("Duplicate webhook config (concurrent create), re-fetching - ownerUserId: {}, repo: {}",
                                ownerUserId, repoFullName);
                        return self.findExistingInNewTransaction(ownerUserId, repoFullName)
                                .orElseThrow(() -> new IllegalStateException(
                                        "Unique constraint violated but config not found after conflict", e));
                    }
                });
    }

    /**
     * Runs in a new transaction so that after a concurrent-insert conflict we can see the committed row.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<GitHubWebhookConfig> findExistingInNewTransaction(Long ownerUserId, String repoFullName) {
        return webhookConfigRepository.findByOwnerUserIdAndRepoFullNameAndEnabledTrue(ownerUserId, repoFullName);
    }

    private String generateTenantKey() {
        byte[] bytes = new byte[12]; // 96 bits
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return "ghw_" + token;
    }
}
