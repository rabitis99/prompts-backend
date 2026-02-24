package org.example.sharedprompts.module.github.port.in;

import org.example.sharedprompts.module.github.dto.body.response.GitHubBodyResponseDto;

import java.util.Optional;

/**
 * GitHub webhook 이벤트 수신 및 처리 유스케이스.
 *
 * 계약:
 * - 입력: tenantKey (URL 경로 식별자), deliveryId (webhook delivery ID), payload (JSON 문자열)
 * - 출력: Optional<GitHubBodyResponseDto>
 * - 설정이 없으면 Optional.empty() 반환 (204 No Content)
 * - 파싱 실패하면 Optional.empty() 반환 (로깅)
 * - 생성 또는 저장 실패하면 예외 발생
 */
public interface ReceiveWebhookUseCase {

  /**
   * GitHub push 이벤트 처리.
   *
   * @param tenantKey webhook 설정의 테넌트 식별자 (ghw_*)
   * @param deliveryId GitHub webhook delivery ID
   * @param payload push 이벤트 JSON 페이로드
   * @return 생성된 본문 또는 empty (설정/파싱 불가능)
   */
  Optional<GitHubBodyResponseDto> handlePush(String tenantKey, String deliveryId, String payload);

  /**
   * GitHub pull_request 이벤트 처리.
   *
   * @param tenantKey webhook 설정의 테넌트 식별자
   * @param deliveryId GitHub webhook delivery ID
   * @param payload pull_request 이벤트 JSON 페이로드
   * @return 생성된 본문 또는 empty (설정/파싱 불가능)
   */
  Optional<GitHubBodyResponseDto> handlePullRequest(String tenantKey, String deliveryId, String payload);
}
