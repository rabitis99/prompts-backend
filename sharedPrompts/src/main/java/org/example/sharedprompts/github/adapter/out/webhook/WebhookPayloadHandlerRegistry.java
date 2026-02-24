package org.example.sharedprompts.github.adapter.out.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.github.domain.model.BodyGenerationRequest;
import org.example.sharedprompts.github.port.out.WebhookParserPort;
import org.example.sharedprompts.github.port.out.WebhookPayloadHandler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Webhook 페이로드 핸들러 레지스트리.
 *
 * 용도: 이벤트 타입별 핸들러 발견 및 라우팅 (OCP 준수).
 * 새로운 이벤트 타입 추가 시: WebhookPayloadHandler 구현체만 추가하면,
 * 여기서 자동으로 감지되어 사용됨 (@Component auto-discovery via Spring).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookPayloadHandlerRegistry implements WebhookParserPort {

  private final List<WebhookPayloadHandler> handlers;

  /**
   * 이벤트 타입에 맞는 핸들러를 찾아서 페이로드 파싱.
   *
   * @param eventType GitHub webhook event type (e.g., "push", "pull_request")
   * @param payload GitHub webhook JSON 페이로드
   * @return 파싱된 본문 생성 요청 또는 empty
   */
  @Override
  public Optional<BodyGenerationRequest> parse(String eventType, String payload) {
    if (eventType == null || payload == null) {
      return Optional.empty();
    }

    Optional<WebhookPayloadHandler> handler = handlers.stream()
        .filter(h -> h.supports(eventType))
        .findFirst();

    if (handler.isEmpty()) {
      log.debug("No handler found for event type: {}", eventType);
      return Optional.empty();
    }

    return handler.get().parse(payload);
  }
}
