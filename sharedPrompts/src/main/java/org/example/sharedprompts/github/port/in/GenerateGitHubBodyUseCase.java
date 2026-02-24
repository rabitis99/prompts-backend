package org.example.sharedprompts.github.port.in;

import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.github.dto.body.response.GitHubBodyResponseDto;
import org.example.sharedprompts.github.dto.body.request.GitHubBodyRequestDto;

/**
 * GitHub Issue/PR 본문 생성 유스케이스.
 *
 * 계약:
 * - 멱등성: 동일한 jobId로 이미 생성된 경우 기존 결과 반환
 * - S3 저장: 생성 후 마크다운을 S3에 저장 (선택사항, nullable)
 * - DB 메타데이터: tenantKey가 있을 경우만 DB에 저장 (webhook 콘텍스트)
 * - 예외: 저장 실패 시 BaseException 발생
 * - 이벤트 타입: "push" | "pull_request" | null
 */
public interface GenerateGitHubBodyUseCase {

  /**
   * 본문 생성 (S3 저장만, DB 메타 저장 안함).
   *
   * @param bodyTemplatePromptId 템플릿 prompt ID (null 시 기본 템플릿 사용)
   * @param request 본문 생성 입력 데이터
   * @return 생성된 Issue/PR 마크다운 및 S3 key (key가 null일 수 있음)
   * @throws IllegalArgumentException jobId 해석 불가
   * @throws Exception 생성 중 예외
   */
  GitHubBodyResponseDto generate(Long bodyTemplatePromptId, GitHubBodyRequestDto request);

  /**
   * 본문 생성 + DB 메타데이터 저장 (webhook 콘텍스트).
   *
   * @param bodyTemplatePromptId 템플릿 prompt ID
   * @param request 본문 생성 입력 데이터
   * @param tenantKey webhook 설정의 테넌트 식별자
   * @param eventType "push" | "pull_request" | null
   * @param ownerUserId 소유자 사용자 ID
   * @return 생성 결과
   * @throws IllegalArgumentException jobId 해석 불가
   * @throws BaseException DB UPSERT 실패
   * @throws Exception 생성 중 예외
   */
  GitHubBodyResponseDto generateWithPersistence(Long bodyTemplatePromptId, GitHubBodyRequestDto request,
                                                 String tenantKey, String eventType, Long ownerUserId);
}
