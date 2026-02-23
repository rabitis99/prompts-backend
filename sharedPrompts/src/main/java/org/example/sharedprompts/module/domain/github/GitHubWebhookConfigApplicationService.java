package org.example.sharedprompts.module.domain.github;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.service.PromptService;
import org.example.sharedprompts.module.domain.github.entity.GitHubWebhookConfig;
import org.example.sharedprompts.module.domain.github.repository.GitHubWebhookConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * GitHub Webhook 설정 관리 애플리케이션 서비스.
 * 인증된 사용자가 promptId + repo 를 넘기면 tenantKey를 생성/재사용하고 Webhook URL 구성을 돕습니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubWebhookConfigApplicationService {

    private final GitHubWebhookConfigRepository webhookConfigRepository;
    private final PromptService promptService;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * ownerUserId + repoFullName 기준으로 설정을 찾고, 없으면 새로 생성해 반환.
     * bodyPromptId는 path의 promptId를 그대로 사용하며, 항상 유효한 prompt인지 검증한다.
     */
    @Transactional
    public GitHubWebhookConfig createOrGet(Long ownerUserId, Long promptId, String repoFullName) {
        // prompt 존재/권한 검증 (조회 가능해야 설정 생성 허용)
        promptService.getPromptDetail(promptId, ownerUserId);

        return webhookConfigRepository
                .findByOwnerUserIdAndRepoFullNameAndEnabledTrue(ownerUserId, repoFullName)
                .orElseGet(() -> {
                    String tenantKey = generateTenantKey();
                    GitHubWebhookConfig config = GitHubWebhookConfig.builder()
                            .tenantKey(tenantKey)
                            .repoFullName(repoFullName)
                            .ownerUserId(ownerUserId)
                            .bodyPromptId(promptId)
                            .enabled(true)
                            .build();
                    GitHubWebhookConfig saved = webhookConfigRepository.save(config);
                    log.info("Created GitHub webhook config - ownerUserId: {}, repo: {}, tenantKey: {}, promptId: {}",
                            ownerUserId, repoFullName, tenantKey, promptId);
                    return saved;
                });
    }

    private String generateTenantKey() {
        byte[] bytes = new byte[12]; // 96 bits
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return "ghw_" + token;
    }
}

