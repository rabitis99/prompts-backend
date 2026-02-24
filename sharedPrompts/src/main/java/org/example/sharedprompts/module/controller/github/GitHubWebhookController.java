package org.example.sharedprompts.module.controller.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.github.GitHubWebhookHandlerService;
import org.example.sharedprompts.module.domain.github.GitHubWebhookSecretResolver;
import org.example.sharedprompts.module.domain.github.GitHubWebhookSignatureVerifier;
import org.example.sharedprompts.module.dto.response.github.GitHubBodyResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * 외부 전용 GitHub Webhook 수신 엔드포인트.
 * URL에 tenantKey를 넣어 사용자별 요청 분리. 설정(tenantKey + repo)이 있으면 본문 생성, 없으면 204.
 * 시크릿이 DB에 있으면 X-Hub-Signature-256 검증 수행, 없으면 검증 생략(통과).
 */
@RestController
@RequestMapping("/webhooks/github")
@RequiredArgsConstructor
@Slf4j
public class GitHubWebhookController {

    private final GitHubWebhookHandlerService webhookHandlerService;
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

        Optional<GitHubBodyResponseDto> result;
        if ("push".equalsIgnoreCase(event)) {
            result = webhookHandlerService.handlePush(tenantKey, delivery, rawPayload);
        } else if ("pull_request".equalsIgnoreCase(event)) {
            result = webhookHandlerService.handlePullRequest(tenantKey, delivery, rawPayload);
        } else {
            log.debug("Ignoring unsupported event: {}", event);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return result
                .map(body -> ResponseEntity.ok(body))
                .orElse(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
    }

    /** payload에서 repository.full_name만 추출 (시크릿 조회용). 파싱 실패 시 null. */
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
