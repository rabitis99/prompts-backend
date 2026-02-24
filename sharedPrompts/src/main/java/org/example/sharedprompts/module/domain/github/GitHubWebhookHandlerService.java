package org.example.sharedprompts.module.domain.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.github.entity.GitHubWebhookConfig;
import org.example.sharedprompts.module.domain.github.repository.GitHubWebhookConfigRepository;
import org.example.sharedprompts.module.dto.request.github.GitHubBodyRequestDto;
import org.example.sharedprompts.module.dto.request.github.webhook.GitHubWebhookPayloads;
import org.example.sharedprompts.module.dto.response.github.GitHubBodyResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * GitHub Webhook 수신 시 tenantKey + repo 로 설정 조회 후, payload를 GitHubBodyRequestDto로 변환해 본문 생성 플로우 실행.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubWebhookHandlerService {

    private final GitHubWebhookConfigRepository webhookConfigRepository;
    private final GitHubBodyGenerateApplicationService bodyGenerateApplicationService;
    private final ObjectMapper objectMapper;

    /**
     * push 이벤트 처리. 설정이 있으면 본문 생성 후 응답, 없으면 empty.
     * generate()가 S3 쓰기를 수행하므로 readOnly 트랜잭션을 사용하지 않음.
     */
    @Transactional
    public Optional<GitHubBodyResponseDto> handlePush(String tenantKey, String deliveryId, String rawPayload) {
        Optional<GitHubWebhookPayloads.PushPayload> payloadOpt = parse(rawPayload, GitHubWebhookPayloads.PushPayload.class);
        if (payloadOpt.isEmpty()) {
            return Optional.empty();
        }
        GitHubWebhookPayloads.PushPayload payload = payloadOpt.get();
        if (payload.getRepository() == null) {
            log.warn("GitHub push payload missing repository");
            return Optional.empty();
        }
        String repoFullName = payload.getRepository().getFullName();
        Optional<GitHubWebhookConfig> configOpt = findConfig(tenantKey, repoFullName);
        if (configOpt.isEmpty()) {
            return Optional.empty();
        }
        GitHubWebhookConfig config = configOpt.get();
        String ref = payload.getRef() != null ? payload.getRef() : "";
        String branch = ref.startsWith("refs/heads/") ? ref.substring("refs/heads/".length()) : ref;
        String sha = payload.getHeadCommit() != null ? payload.getHeadCommit().getId() : "";
        String title = payload.getHeadCommit() != null && payload.getHeadCommit().getMessage() != null
                ? firstLine(payload.getHeadCommit().getMessage()) : "";
        String author = payload.getHeadCommit() != null && payload.getHeadCommit().getAuthor() != null
                ? (payload.getHeadCommit().getAuthor().getUsername() != null ? payload.getHeadCommit().getAuthor().getUsername() : payload.getHeadCommit().getAuthor().getName())
                : "";
        String date = payload.getHeadCommit() != null ? nullToEmpty(payload.getHeadCommit().getTimestamp()) : "";
        String commits = formatCommits(payload.getCommits());
        if (commits.isEmpty() && payload.getHeadCommit() != null && payload.getHeadCommit().getMessage() != null) {
            commits = payload.getHeadCommit().getMessage();
        }
        GitHubBodyRequestDto dto = new GitHubBodyRequestDto(
                null,
                deliveryId,
                sha,
                repoFullName,
                branch,
                "",
                title,
                author,
                date,
                commits,
                "",
                tenantKey
        );
        GitHubBodyResponseDto response = bodyGenerateApplicationService.generate(
                config.getBodyPromptId(), dto, tenantKey, "push", config.getOwnerUserId());
        return Optional.of(response);
    }

    /**
     * pull_request 이벤트 처리 (opened, synchronize 등). 설정이 있으면 본문 생성.
     * generate()가 S3 쓰기를 수행하므로 readOnly 트랜잭션을 사용하지 않음.
     */
    @Transactional
    public Optional<GitHubBodyResponseDto> handlePullRequest(String tenantKey, String deliveryId, String rawPayload) {
        Optional<GitHubWebhookPayloads.PullRequestPayload> payloadOpt = parse(rawPayload, GitHubWebhookPayloads.PullRequestPayload.class);
        if (payloadOpt.isEmpty()) {
            return Optional.empty();
        }
        GitHubWebhookPayloads.PullRequestPayload payload = payloadOpt.get();
        if (payload.getRepository() == null || payload.getPullRequest() == null) {
            log.warn("GitHub pull_request payload missing repository or pull_request");
            return Optional.empty();
        }
        String repoFullName = payload.getRepository().getFullName();
        Optional<GitHubWebhookConfig> configOpt = findConfig(tenantKey, repoFullName);
        if (configOpt.isEmpty()) {
            return Optional.empty();
        }
        GitHubWebhookConfig config = configOpt.get();
        GitHubWebhookPayloads.PullRequest pr = payload.getPullRequest();
        String sha = pr.getHead() != null ? pr.getHead().getSha() : "";
        String branch = pr.getHead() != null ? pr.getHead().getRef() : "";
        String baseBranch = pr.getBase() != null ? pr.getBase().getRef() : "";
        String title = pr.getTitle() != null ? pr.getTitle() : "";
        String author = pr.getUser() != null ? pr.getUser().getLogin() : "";
        String date = pr.getCreatedAt() != null ? pr.getCreatedAt() : "";
        GitHubBodyRequestDto dto = new GitHubBodyRequestDto(
                null,
                deliveryId,
                sha,
                repoFullName,
                branch,
                baseBranch,
                title,
                author,
                date,
                "",
                "",
                tenantKey
        );
        GitHubBodyResponseDto response = bodyGenerateApplicationService.generate(
                config.getBodyPromptId(), dto, tenantKey, "pull_request", config.getOwnerUserId());
        return Optional.of(response);
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    private static String firstLine(String s) {
        if (s == null) return "";
        int i = s.indexOf('\n');
        return i >= 0 ? s.substring(0, i).trim() : s.trim();
    }

    private static String formatCommits(List<GitHubWebhookPayloads.HeadCommit> commits) {
        if (commits == null || commits.isEmpty()) return "";
        return commits.stream()
                .map(c -> c != null && c.getMessage() != null ? c.getMessage() : "")
                .map(GitHubWebhookHandlerService::firstLine)
                .collect(Collectors.joining("\n"));
    }

    private Optional<GitHubWebhookConfig> findConfig(String tenantKey, String repoFullName) {
        Optional<GitHubWebhookConfig> configOpt = webhookConfigRepository
                .findByTenantKeyAndRepoFullNameAndEnabledTrue(tenantKey, repoFullName);
        if (configOpt.isEmpty()) {
            log.debug("No webhook config for tenantKey={}, repo={}", tenantKey, repoFullName);
        }
        return configOpt;
    }

    private <T> Optional<T> parse(String raw, Class<T> type) {
        try {
            return Optional.ofNullable(objectMapper.readValue(raw, type));
        } catch (Exception e) {
            log.warn("Failed to parse GitHub webhook payload: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
