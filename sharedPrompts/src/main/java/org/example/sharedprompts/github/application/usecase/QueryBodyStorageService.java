package org.example.sharedprompts.github.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.example.sharedprompts.github.domain.model.GithubBodyStorage;
import org.example.sharedprompts.github.dto.body.response.GitHubBodyDownloadUrlDto;
import org.example.sharedprompts.github.dto.body.response.GitHubBodyPreviewResponseDto;
import org.example.sharedprompts.github.dto.body.response.GitHubBodyStorageListDto;
import org.example.sharedprompts.github.port.in.QueryStoredBodyUseCase;
import org.example.sharedprompts.github.port.out.BodyStoragePersistencePort;
import org.example.sharedprompts.github.port.out.StoragePort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * 저장된 GitHub 본문 조회 유스케이스 구현.
 *
 * 역할:
 * - DB 메타데이터 조회
 * - 접근제어 (소유자 검증)
 * - S3 다운로드 (미리보기)
 * - 프리사인드 URL 생성 (다운로드)
 *
 * 마이그레이션:
 * - 기존 GitHubBodyStorageApplicationService → 이 서비스로 통합
 * - StorageFacade → StoragePort 호출
 * - GithubBodyStorageRepository → BodyStoragePersistencePort 호출
 *
 * @see QueryStoredBodyUseCase
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueryBodyStorageService implements QueryStoredBodyUseCase {

  private static final Duration PRESIGNED_URL_TTL = Duration.ofMinutes(15);

  private final BodyStoragePersistencePort persistencePort;
  private final StoragePort storagePort;

  @Transactional(readOnly = true)
  @Override
  public Page<GitHubBodyStorageListDto> listByOwner(Long ownerUserId, Long currentUserId, Pageable pageable) {
    if (ownerUserId == null || !ownerUserId.equals(currentUserId)) {
      throw new BaseException(ModuleErrorCode.GITHUB_BODY_STORAGE_FORBIDDEN, null, "owner mismatch");
    }

    Page<GithubBodyStorage> page = persistencePort.findByOwner(ownerUserId, pageable);
    return page.map(this::toListDto);
  }

  @Transactional(readOnly = true)
  @Override
  public GitHubBodyPreviewResponseDto preview(Long storageId, Long currentUserId, String type) {
    GithubBodyStorage entity = getEntityAndCheckOwner(storageId, currentUserId);
    String s3Key = resolveS3KeyByType(entity, type);
    if (s3Key == null || s3Key.isBlank()) {
      return new GitHubBodyPreviewResponseDto("");
    }
    String markdown = storagePort.downloadBody(s3Key);
    return new GitHubBodyPreviewResponseDto(markdown != null ? markdown : "");
  }

  @Transactional(readOnly = true)
  @Override
  public GitHubBodyDownloadUrlDto getDownloadUrl(Long storageId, Long currentUserId, String type) {
    GithubBodyStorage entity = getEntityAndCheckOwner(storageId, currentUserId);
    String s3Key = resolveS3KeyByType(entity, type);
    if (s3Key == null || s3Key.isBlank()) {
      throw new BaseException(ModuleErrorCode.GITHUB_BODY_STORAGE_NOT_FOUND, null,
          "No stored file key for type: " + type);
    }
    String url = storagePort.generatePresignedUrl(s3Key, PRESIGNED_URL_TTL);
    return new GitHubBodyDownloadUrlDto(url);
  }

  private GithubBodyStorage getEntityAndCheckOwner(Long storageId, Long currentUserId) {
    GithubBodyStorage entity = persistencePort.findById(storageId)
        .orElseThrow(() -> new BaseException(ModuleErrorCode.GITHUB_BODY_STORAGE_NOT_FOUND, null,
            "id: " + storageId));
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

  private GitHubBodyStorageListDto toListDto(GithubBodyStorage entity) {
    return new GitHubBodyStorageListDto(
        entity.getId(),
        entity.getTenantKey(),
        entity.getRepoFullName(),
        entity.getJobId(),
        entity.getEventType(),
        entity.getCreatedAt()
    );
  }
}
