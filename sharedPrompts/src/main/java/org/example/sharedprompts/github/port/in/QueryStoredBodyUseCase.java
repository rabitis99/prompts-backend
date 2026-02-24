package org.example.sharedprompts.github.port.in;

import org.example.sharedprompts.github.dto.body.response.GitHubBodyStorageListDto;
import org.example.sharedprompts.github.dto.body.response.GitHubBodyPreviewResponseDto;
import org.example.sharedprompts.github.dto.body.response.GitHubBodyDownloadUrlDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 저장된 GitHub 본문 조회 및 접근 유스케이스.
 *
 * 계약:
 * - 접근제어: 소유자만 조회 가능
 * - 프리사인드 URL: 15분 TTL S3 다운로드 URL
 * - 프리뷰: S3에서 마크다운 내용 다운로드
 * - 예외: 접근 불가 또는 미존재 시 예외
 */
public interface QueryStoredBodyUseCase {

  /**
   * 사용자의 저장된 본문 목록 (페이지네이션, 최신순).
   *
   * @param ownerUserId 조회 대상 사용자
   * @param currentUserId 현재 사용자 (접근제어)
   * @param pageable 페이징 정보
   * @return 본문 메타데이터 페이지
   * @throws ForbiddenException ownerUserId != currentUserId
   */
  Page<GitHubBodyStorageListDto> listByOwner(Long ownerUserId, Long currentUserId, Pageable pageable);

  /**
   * 저장된 본문 프리뷰 (S3에서 다운로드).
   *
   * @param storageId 저장소 레코드 ID
   * @param currentUserId 현재 사용자
   * @param type "issue" | "pr"
   * @return 마크다운 내용
   * @throws BadRequestException type 미지원
   * @throws NotFoundException 저장소 레코드 미존재
   * @throws ForbiddenException 소유자가 아님
   */
  GitHubBodyPreviewResponseDto preview(Long storageId, Long currentUserId, String type);

  /**
   * S3 프리사인드 다운로드 URL 생성 (15분 TTL).
   *
   * @param storageId 저장소 레코드 ID
   * @param currentUserId 현재 사용자
   * @param type "issue" | "pr"
   * @return 프리사인드 URL
   * @throws BadRequestException type 미지원
   * @throws NotFoundException 저장소 레코드 미존재
   * @throws ForbiddenException 소유자가 아님
   */
  GitHubBodyDownloadUrlDto getDownloadUrl(Long storageId, Long currentUserId, String type);
}
