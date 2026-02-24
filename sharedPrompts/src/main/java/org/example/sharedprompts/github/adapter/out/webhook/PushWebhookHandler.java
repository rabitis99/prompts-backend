package org.example.sharedprompts.github.adapter.out.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.github.domain.model.BodyGenerationRequest;
import org.example.sharedprompts.github.dto.webhook.request.GitHubWebhookPayloads;
import org.example.sharedprompts.github.port.out.WebhookPayloadHandler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * GitHub push 이벤트 핸들러.
 * Webhook 페이로드 파싱 → BodyGenerationRequest 변환.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PushWebhookHandler implements WebhookPayloadHandler {

  private final ObjectMapper objectMapper;

  @Override
  public boolean supports(String eventType) {
    return "push".equalsIgnoreCase(eventType);
  }

  @Override
  public Optional<BodyGenerationRequest> parse(String payload) {
    try {
      GitHubWebhookPayloads.PushPayload pushPayload = objectMapper.readValue(
          payload, GitHubWebhookPayloads.PushPayload.class);

      if (pushPayload.getRepository() == null) {
        log.debug("Push payload missing repository");
        return Optional.empty();
      }

      String repoFullName = pushPayload.getRepository().getFullName();
      String ref = pushPayload.getRef() != null ? pushPayload.getRef() : "";
      String branch = ref.startsWith("refs/heads/")
          ? ref.substring("refs/heads/".length())
          : ref;
      String sha = pushPayload.getHeadCommit() != null
          ? pushPayload.getHeadCommit().getId()
          : "";
      String title = extractFirstLine(
          pushPayload.getHeadCommit() != null ? pushPayload.getHeadCommit().getMessage() : null);
      String author = extractAuthor(pushPayload.getHeadCommit());
      String date = extractDate(pushPayload.getHeadCommit());
      String commits = formatCommits(pushPayload.getCommits());

      if (commits.isEmpty() && pushPayload.getHeadCommit() != null
          && pushPayload.getHeadCommit().getMessage() != null) {
        commits = pushPayload.getHeadCommit().getMessage();
      }

      return Optional.of(BodyGenerationRequest.builder()
          .deliveryId(null) // delivery ID는 webhook 컨텍스트에서 설정
          .sha(sha)
          .repoFullName(repoFullName)
          .branch(branch)
          .baseBranch("")
          .title(title)
          .author(author)
          .date(date)
          .commits(commits)
          .files("")
          .build());

    } catch (Exception e) {
      log.warn("Failed to parse push webhook: {}", e.getMessage());
      return Optional.empty();
    }
  }

  private String extractFirstLine(String text) {
    if (text == null || text.isBlank()) return "";
    int newlineIndex = text.indexOf('\n');
    return newlineIndex >= 0 ? text.substring(0, newlineIndex).trim() : text.trim();
  }

  private String extractAuthor(GitHubWebhookPayloads.HeadCommit commit) {
    if (commit == null || commit.getAuthor() == null) return "";
    GitHubWebhookPayloads.Author author = commit.getAuthor();
    if (author.getUsername() != null) return author.getUsername();
    return author.getName() != null ? author.getName() : "";
  }

  private String extractDate(GitHubWebhookPayloads.HeadCommit commit) {
    return commit != null && commit.getTimestamp() != null ? commit.getTimestamp() : "";
  }

  private String formatCommits(List<GitHubWebhookPayloads.HeadCommit> commits) {
    if (commits == null || commits.isEmpty()) return "";
    return commits.stream()
        .filter(c -> c != null && c.getMessage() != null)
        .map(c -> extractFirstLine(c.getMessage()))
        .collect(Collectors.joining("\n"));
  }
}
