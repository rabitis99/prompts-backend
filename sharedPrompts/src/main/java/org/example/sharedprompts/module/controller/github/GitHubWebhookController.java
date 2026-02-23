package org.example.sharedprompts.module.controller.github;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.github.GitHubWebhookHandlerService;
import org.example.sharedprompts.module.dto.response.github.GitHubBodyResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * 외부 전용 GitHub Webhook 수신 엔드포인트.
 * URL에 tenantKey를 넣어 사용자별 요청 분리. 설정(tenantKey + repo)이 있으면 본문 생성, 없으면 204.
 */
@RestController
@RequestMapping("webhooks/github")
@RequiredArgsConstructor
@Slf4j
public class GitHubWebhookController {

    private final GitHubWebhookHandlerService webhookHandlerService;

    @PostMapping("/{tenantKey}")
    public ResponseEntity<?> handle(
            @PathVariable String tenantKey,
            @RequestHeader(value = "X-GitHub-Event", required = false) String event,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
            @RequestBody String rawPayload
    ) {
        if (tenantKey == null || tenantKey.isBlank()) {
            return ResponseEntity.badRequest().build();
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
}
