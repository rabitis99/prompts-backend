package org.example.sharedprompts.github.port.in;

import org.example.sharedprompts.github.domain.model.GitHubWebhookConfig;

/**
 * Webhook 설정 생성/조회 유스케이스.
 *
 * 계약:
 * - 사용자별, 리포지토리별 단일 설정 관리
 * - 설정 없으면 생성, 있으면 업데이트
 * - tenantKey 생성 (보안): Base64UrlEncoded "ghw_" prefix
 * - Prompt ID 검증: 존재하지 않으면 예외
 * - 동시 생성 처리 (DataIntegrityViolationException → retry)
 */
public interface CreateWebhookConfigUseCase {

  /**
   * 웹훅 설정 생성 또는 조회.
   *
   * @param ownerUserId 설정 소유자
   * @param promptId 본문 생성 템플릿 prompt ID
   * @param repoFullName GitHub 저장소 (owner/repo)
   * @param webhookSecret GitHub webhook secret (signature 검증용)
   * @return 설정 엔티티 (기존 또는 신규)
   * @throws IllegalArgumentException promptId 검증 실패
   * @throws IllegalStateException 설정 생성/조회 실패
   */
  GitHubWebhookConfig createOrGet(Long ownerUserId, Long promptId, String repoFullName, String webhookSecret);
}
