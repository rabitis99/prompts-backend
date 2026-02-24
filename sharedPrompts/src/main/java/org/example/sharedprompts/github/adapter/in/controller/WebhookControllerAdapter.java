package org.example.sharedprompts.github.adapter.in.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.github.domain.service.GitHubWebhookSecretResolver;
import org.example.sharedprompts.github.domain.service.GitHubWebhookSignatureVerifier;
import org.example.sharedprompts.github.dto.body.response.GitHubBodyResponseDto;
import org.example.sharedprompts.github.port.in.ReceiveWebhookUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * GitHub Webhook 수신 input adapter.
 *
 * 책임:
 * - HTTP 요청 수신
 * - Signature 검증 (보안)
 * - ReceiveWebhookUseCase (Port) 호출
 * - HTTP 응답 반환
 *
 * 변경점:
 * - GitHubWebhookHandlerService → ReceiveWebhookUseCase (Port)
 * - 이벤트 처리는 Port 구현체에서 담당
 *
 * 스펙 유지:
 * - 경로: /webhooks/github/{tenantKey} (POST)
 * - 응답: 200 (본문), 204 (설정 없음), 401 (서명 실패)
 */
@RestController
@RequestMapping("/webhooks/github")
@RequiredArgsConstructor
@Slf4j
public class WebhookControllerAdapter {

  private final ReceiveWebhookUseCase receiveWebhookUseCase;
  private final GitHubWebhookSecretResolver secretResolver;
  private final GitHubWebhookSignatureVerifier signatureVerifier;
  private final ObjectMapper objectMapper;

  @PostMapping("/{tenantKey}")
  public ResponseEntity<?> handle(
      @PathVariable String tenantKey,
      @RequestHeader(value = "X-GitHub-Event", required = false) String event,
      @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
      @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature256,
      @RequestBody String rawPayload
  ) {
    String repoFullName = parseRepoFullName(rawPayload);
    var secretOpt = secretResolver.resolveSecret(tenantKey, repoFullName);
    if (secretOpt.isPresent() && !signatureVerifier.verify(rawPayload, signature256, secretOpt.get())) {
      log.warn("GitHub webhook signature verification failed - tenantKey: {}", tenantKey);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String delivery = deliveryId != null && !deliveryId.isBlank() ? deliveryId : "unknown";
    log.info("GitHub webhook received - tenantKey: {}, event: {}, delivery: {}", tenantKey, event, delivery);

    Optional<GitHubBodyResponseDto> result = switch (event != null ? event.toLowerCase() : "") {
      case "push" -> receiveWebhookUseCase.handlePush(tenantKey, delivery, rawPayload);
      case "pull_request" -> receiveWebhookUseCase.handlePullRequest(tenantKey, delivery, rawPayload);
      default -> {
        log.debug("Ignoring unsupported event: {}", event);
        yield Optional.empty();
      }
    };

    return result
        .map(body -> ResponseEntity.ok((Object) body))
        .orElse(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
  }

  private String parseRepoFullName(String rawPayload) {
    if (rawPayload == null || rawPayload.isBlank()) return null;
    try {
      JsonNode root = objectMapper.readTree(rawPayload);
      JsonNode repo = root != null ? root.get("repository") : null;
      JsonNode fullName = repo != null ? repo.get("full_name") : null;
      return fullName != null && fullName.isTextual() ? fullName.asText() : null;
    } catch (Exception e) {
      return null;
    }
  }
}
