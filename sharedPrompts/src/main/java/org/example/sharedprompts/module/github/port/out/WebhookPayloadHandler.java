package org.example.sharedprompts.module.github.port.out;

import org.example.sharedprompts.module.github.domain.model.BodyGenerationRequest;

import java.util.Optional;

/**
 * Webhook 페이로드 핸들러 (Registry pattern).
 *
 * 용도: GitHub 이벤트 타입별로 다른 파서를 적용하기 위한 extension point.
 * OCP 준수: 새로운 이벤트 타입 추가 시 이 인터페이스 구현체만 추가하면 됨,
 * 기존 switch/if 로직 수정 불필요 (registry에서 자동 발견).
 *
 * 구현 책임:
 * - {@link org.example.sharedprompts.module.github.adapter.out.webhook.PushWebhookHandler}
 * - {@link org.example.sharedprompts.module.github.adapter.out.webhook.PullRequestWebhookHandler}
 * - (각 이벤트 타입별 handler 클래스)
 *
 * LSP 계약:
 * - supports(): 정확한 이벤트 타입만 true 반환
 * - parse(): 파싱 가능하면 Optional.of(), 파싱 실패하면 Optional.empty()
 *   예외 발생 불가 (로깅 후 empty 반환)
 */
public interface WebhookPayloadHandler {

  /**
   * 이 핸들러가 처리 가능한 이벤트 타입인지 확인.
   *
   * @param eventType GitHub webhook event type (예: "push", "pull_request")
   * @return true if this handler can process
   */
  boolean supports(String eventType);

  /**
   * Webhook 페이로드 파싱.
   *
   * @param payload GitHub webhook JSON 페이로드 (raw string)
   * @return 파싱된 요청 또는 empty (파싱 실패 시)
   *         예외 발생하지 않음, 모든 예외는 로깅 후 empty로 변환
   */
  Optional<BodyGenerationRequest> parse(String payload);
}
