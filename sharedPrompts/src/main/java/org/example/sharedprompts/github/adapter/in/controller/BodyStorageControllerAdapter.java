package org.example.sharedprompts.github.adapter.in.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.dto.common.PageResponse;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.example.sharedprompts.github.dto.body.response.GitHubBodyDownloadUrlDto;
import org.example.sharedprompts.github.dto.body.response.GitHubBodyPreviewResponseDto;
import org.example.sharedprompts.github.dto.body.response.GitHubBodyStorageListDto;
import org.example.sharedprompts.github.port.in.QueryStoredBodyUseCase;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * GitHub 본문 저장소 조회 input adapter.
 *
 * 책임:
 * - HTTP 요청 수신
 * - 페이지네이션, 인증 처리
 * - QueryStoredBodyUseCase (Port) 호출
 * - HTTP 응답 반환
 *
 * 변경점:
 * - GitHubBodyStorageApplicationService → QueryStoredBodyUseCase (Port)
 * - 모든 비즈니스 로직은 Port 구현체에서 담당
 *
 * 스펙 유지:
 * - 경로: /github/bodies, /github/bodies/{id}, /github/bodies/{id}/download
 * - 메서드: GET
 * - 응답: 페이지 (list), 마크다운 (preview), 다운로드 URL (download)
 */
@RestController
@RequestMapping("/github/bodies")
@RequiredArgsConstructor
@Slf4j
public class BodyStorageControllerAdapter {

  private static final int MAX_PAGE_SIZE = 100;

  private final QueryStoredBodyUseCase queryStoredBodyUseCase;

  @GetMapping
  public ResponseEntity<CustomResponse<PageResponse<GitHubBodyStorageListDto>>> list(
      @RequestParam Long ownerUserId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @CurrentUser AuthUser authUser
  ) {
    if (!authUser.getId().equals(ownerUserId)) {
      throw new BaseException(ModuleErrorCode.GITHUB_BODY_STORAGE_FORBIDDEN, null, "ownerUserId mismatch");
    }
    int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
    Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize);
    var dtoPage = queryStoredBodyUseCase.listByOwner(ownerUserId, authUser.getId(), pageable);
    return CustomResponseHelper.ok(PageResponse.of(dtoPage));
  }

  @GetMapping("/{id}")
  public ResponseEntity<CustomResponse<GitHubBodyPreviewResponseDto>> preview(
      @PathVariable Long id,
      @RequestParam(defaultValue = "issue") String type,
      @CurrentUser AuthUser authUser
  ) {
    GitHubBodyPreviewResponseDto dto = queryStoredBodyUseCase.preview(id, authUser.getId(), type);
    return CustomResponseHelper.ok(dto);
  }

  @GetMapping("/{id}/download")
  public ResponseEntity<CustomResponse<GitHubBodyDownloadUrlDto>> download(
      @PathVariable Long id,
      @RequestParam(defaultValue = "issue") String type,
      @CurrentUser AuthUser authUser
  ) {
    GitHubBodyDownloadUrlDto dto = queryStoredBodyUseCase.getDownloadUrl(id, authUser.getId(), type);
    return CustomResponseHelper.ok(dto);
  }

  @GetMapping(value = "/{id}/download", params = "redirect=true")
  public ResponseEntity<Void> downloadRedirect(
      @PathVariable Long id,
      @RequestParam(defaultValue = "issue") String type,
      @CurrentUser AuthUser authUser
  ) {
    GitHubBodyDownloadUrlDto dto = queryStoredBodyUseCase.getDownloadUrl(id, authUser.getId(), type);
    String url = dto.downloadUrl();
    if (url == null || url.isBlank()) {
      throw new BaseException(ModuleErrorCode.STORAGE_ERROR, null, "Download URL could not be generated for redirect");
    }
    return ResponseEntity.status(302)
        .header(HttpHeaders.LOCATION, url)
        .build();
  }
}
