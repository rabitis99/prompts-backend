package org.example.sharedprompts.github.application.body;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.application.storage.StorageFacade;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.example.sharedprompts.github.domain.model.GithubBodyStorage;
import org.example.sharedprompts.github.domain.repository.GithubBodyStorageRepository;
import org.example.sharedprompts.github.domain.service.GitHubBodyStorageService;
import org.example.sharedprompts.github.dto.body.response.GitHubBodyDownloadUrlDto;
import org.example.sharedprompts.github.dto.body.response.GitHubBodyPreviewResponseDto;
import org.example.sharedprompts.github.dto.body.response.GitHubBodyStorageListDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * GitHub 본문 저장 목록/미리보기/다운로드 애플리케이션 서비스.
 * DB 메타데이터 조회 및 권한 검사 후 S3 본문 조회 또는 Presigned URL 생성을 수행합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubBodyStorageApplicationService {

    private static final Duration PRESIGNED_URL_TTL = Duration.ofMinutes(15);

    private final GithubBodyStorageRepository githubBodyStorageRepository;
    private final GitHubBodyStorageService storageService;
    private final StorageFacade storageFacade;

    /**
     * 소유자 기준 저장 목록 페이징 조회. 최신순(created_at DESC).
     * 권한 검사는 Controller에서 ownerUserId와 현재 사용자 일치 여부로 수행합니다.
     */
    @Transactional(readOnly = true)
    public Page<GitHubBodyStorageListDto> listByOwnerUserId(Long ownerUserId, Pageable pageable) {
        Page<GithubBodyStorage> page = githubBodyStorageRepository.findByOwnerUserIdOrderByCreatedAtDesc(ownerUserId, pageable);
        return page.map(this::toListDto);
    }

    /**
     * 단건 조회 후 소유자 일치 시 본문 미리보기(마크다운 문자열) 반환.
     * type=issue | pr 에 따라 해당 S3 키로 다운로드 후 반환합니다.
     *
     * @throws BaseException GITHUB_BODY_STORAGE_NOT_FOUND 또는 GITHUB_BODY_STORAGE_FORBIDDEN
     */
    @Transactional(readOnly = true)
    public GitHubBodyPreviewResponseDto preview(Long id, Long currentUserId, String type) {
        GithubBodyStorage entity = getEntityAndCheckOwner(id, currentUserId);
        String s3Key = resolveS3KeyByType(entity, type);
        String markdown = storageService.downloadBody(s3Key);
        return new GitHubBodyPreviewResponseDto(markdown != null ? markdown : "");
    }

    /**
     * 단건 조회 후 소유자 일치 시 다운로드용 Presigned URL 생성.
     * 302 리다이렉트용 URL 또는 JSON 응답용 DTO로 반환합니다.
     *
     * @throws BaseException GITHUB_BODY_STORAGE_NOT_FOUND 또는 GITHUB_BODY_STORAGE_FORBIDDEN
     */
    @Transactional(readOnly = true)
    public GitHubBodyDownloadUrlDto getDownloadUrl(Long id, Long currentUserId, String type) {
        GithubBodyStorage entity = getEntityAndCheckOwner(id, currentUserId);
        String s3Key = resolveS3KeyByType(entity, type);
        if (s3Key == null || s3Key.isBlank()) {
            throw new BaseException(ModuleErrorCode.GITHUB_BODY_STORAGE_NOT_FOUND, null,
                    "No stored file key for type: " + type);
        }
        String url = storageFacade.generateDownloadUrl(s3Key, PRESIGNED_URL_TTL);
        return new GitHubBodyDownloadUrlDto(url);
    }

    private GithubBodyStorage getEntityAndCheckOwner(Long id, Long currentUserId) {
        GithubBodyStorage entity = githubBodyStorageRepository.findById(id)
                .orElseThrow(() -> new BaseException(ModuleErrorCode.GITHUB_BODY_STORAGE_NOT_FOUND, null, "id: " + id));
        if (entity.getOwnerUserId() == null || !entity.getOwnerUserId().equals(currentUserId)) {
            throw new BaseException(ModuleErrorCode.GITHUB_BODY_STORAGE_FORBIDDEN, null, "owner mismatch");
        }
        return entity;
    }

    private String resolveS3KeyByType(GithubBodyStorage entity, String type) {
        if ("pr".equalsIgnoreCase(type)) {
            return entity.getStoredPrFileKey();
        }
        if (!"issue".equalsIgnoreCase(type)) {
            throw new BaseException(ModuleErrorCode.VALIDATION_ERROR, null, "Invalid type: " + type);
        }
        return entity.getStoredIssueFileKey();
    }

    private GitHubBodyStorageListDto toListDto(GithubBodyStorage e) {
        return new GitHubBodyStorageListDto(
                e.getId(),
                e.getTenantKey(),
                e.getRepoFullName(),
                e.getJobId(),
                e.getEventType(),
                e.getCreatedAt()
        );
    }
}
