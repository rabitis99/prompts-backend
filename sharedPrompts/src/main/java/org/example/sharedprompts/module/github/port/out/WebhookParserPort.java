package org.example.sharedprompts.module.github.port.out;

import org.example.sharedprompts.module.github.domain.model.BodyGenerationRequest;

import java.util.Optional;

/**
 * Webhook 페이로드 파싱 포트.
 *
 * 역할:
 * - GitHub webhook event + payload를 도메인 모델 {@link BodyGenerationRequest}로 변환.
 * - 애플리케이션 계층(유스케이스)이 adapter 구현체에 직접 의존하지 않도록 추상화.
 */
public interface WebhookParserPort {

  /**
   * 이벤트 타입과 페이로드를 파싱해 본문 생성 요청으로 변환.
   *
   * @param eventType GitHub webhook event type (e.g., "push", "pull_request")
   * @param payload GitHub webhook JSON payload
   * @return 파싱된 본문 생성 요청 또는 empty
   */
  Optional<BodyGenerationRequest> parse(String eventType, String payload);
}

