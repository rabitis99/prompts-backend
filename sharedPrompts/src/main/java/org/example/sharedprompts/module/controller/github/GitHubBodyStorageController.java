package org.example.sharedprompts.module.controller.github;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.dto.common.PageResponse;
import org.example.sharedprompts.module.domain.github.GitHubBodyStorageApplicationService;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.example.sharedprompts.module.dto.response.github.GitHubBodyDownloadUrlDto;
import org.example.sharedprompts.module.dto.response.github.GitHubBodyPreviewResponseDto;
import org.example.sharedprompts.module.dto.response.github.GitHubBodyStorageListDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * GitHub 본문 S3 저장 목록/미리보기/다운로드 API.
 * 인증된 사용자가 자신의 저장(owner_user_id 일치)에 대해서만 접근 가능합니다.
 */
@RestController
@RequestMapping("/github/bodies")
@RequiredArgsConstructor
@Slf4j
public class GitHubBodyStorageController {

    private final GitHubBodyStorageApplicationService bodyStorageApplicationService;

    /**
     * 목록 조회. ownerUserId(필수) 기준 페이징, 최신순.
     * 요청한 ownerUserId가 현재 로그인 사용자와 일치해야 합니다.
     */
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
        Pageable pageable = PageRequest.of(page, size);
        var dtoPage = bodyStorageApplicationService.listByOwnerUserId(ownerUserId, pageable);
        return CustomResponseHelper.ok(PageResponse.of(dtoPage));
    }

    /**
     * 미리보기. type=issue | pr 에 따라 해당 본문 마크다운을 반환합니다.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CustomResponse<GitHubBodyPreviewResponseDto>> preview(
            @PathVariable Long id,
            @RequestParam(defaultValue = "issue") String type,
            @CurrentUser AuthUser authUser
    ) {
        GitHubBodyPreviewResponseDto dto = bodyStorageApplicationService.preview(id, authUser.getId(), type);
        return CustomResponseHelper.ok(dto);
    }

    /**
     * 다운로드. Presigned URL을 JSON으로 반환합니다.
     * (302 리다이렉트가 필요하면 이 URL로 프론트에서 location 이동 처리)
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<CustomResponse<GitHubBodyDownloadUrlDto>> download(
            @PathVariable Long id,
            @RequestParam(defaultValue = "issue") String type,
            @CurrentUser AuthUser authUser
    ) {
        GitHubBodyDownloadUrlDto dto = bodyStorageApplicationService.getDownloadUrl(id, authUser.getId(), type);
        return CustomResponseHelper.ok(dto);
    }

    /**
     * 다운로드 Presigned URL로 302 리다이렉트.
     * 브라우저에서 직접 다운로드 링크로 쓸 때 사용합니다.
     */
    @GetMapping(value = "/{id}/download", params = "redirect=true")
    public ResponseEntity<Void> downloadRedirect(
            @PathVariable Long id,
            @RequestParam(defaultValue = "issue") String type,
            @CurrentUser AuthUser authUser
    ) {
        GitHubBodyDownloadUrlDto dto = bodyStorageApplicationService.getDownloadUrl(id, authUser.getId(), type);
        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, dto.downloadUrl())
                .build();
    }
}
