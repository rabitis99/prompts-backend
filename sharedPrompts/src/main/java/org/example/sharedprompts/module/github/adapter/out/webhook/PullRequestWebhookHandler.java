package org.example.sharedprompts.module.github.adapter.out.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.github.domain.model.BodyGenerationRequest;
import org.example.sharedprompts.module.github.dto.webhook.request.GitHubWebhookPayloads;
import org.example.sharedprompts.module.github.port.out.WebhookPayloadHandler;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * GitHub pull_request 이벤트 핸들러.
 * Webhook 페이로드 파싱 → BodyGenerationRequest 변환.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PullRequestWebhookHandler implements WebhookPayloadHandler {

  private final ObjectMapper objectMapper;

  @Override
  public boolean supports(String eventType) {
    return "pull_request".equalsIgnoreCase(eventType);
  }

  @Override
  public Optional<BodyGenerationRequest> parse(String payload) {
    try {
      GitHubWebhookPayloads.PullRequestPayload prPayload = objectMapper.readValue(
          payload, GitHubWebhookPayloads.PullRequestPayload.class);

      if (prPayload.getRepository() == null || prPayload.getPullRequest() == null) {
        log.debug("PR payload missing repository or pullRequest");
        return Optional.empty();
      }

      String repoFullName = prPayload.getRepository().getFullName();
      GitHubWebhookPayloads.PullRequest pr = prPayload.getPullRequest();
      String sha = pr.getHead() != null ? pr.getHead().getSha() : "";
      String branch = pr.getHead() != null ? pr.getHead().getRef() : "";
      String baseBranch = pr.getBase() != null ? pr.getBase().getRef() : "";
      String title = pr.getTitle() != null ? pr.getTitle() : "";
      String author = pr.getUser() != null ? pr.getUser().getLogin() : "";
      String date = pr.getCreatedAt() != null ? pr.getCreatedAt() : "";

      return Optional.of(BodyGenerationRequest.builder()
          .deliveryId(null) // delivery ID는 webhook 컨텍스트에서 설정
          .sha(sha)
          .repoFullName(repoFullName)
          .branch(branch)
          .baseBranch(baseBranch)
          .title(title)
          .author(author)
          .date(date)
          .commits("")
          .files("")
          .build());

    } catch (Exception e) {
      log.warn("Failed to parse pull_request webhook: {}", e.getMessage());
      return Optional.empty();
    }
  }
}
