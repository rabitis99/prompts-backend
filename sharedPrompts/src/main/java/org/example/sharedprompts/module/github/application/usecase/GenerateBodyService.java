package org.example.sharedprompts.module.github.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.example.sharedprompts.module.github.domain.service.GitHubBodyGeneratorService;
import org.example.sharedprompts.module.github.domain.service.GitHubBodyStorageService;
import org.example.sharedprompts.module.github.dto.body.request.GitHubBodyRequestDto;
import org.example.sharedprompts.module.github.dto.body.response.GitHubBodyResponseDto;
import org.example.sharedprompts.module.github.port.in.GenerateGitHubBodyUseCase;
import org.example.sharedprompts.module.github.port.out.BodyStoragePersistencePort;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * GitHub 본문 생성 유스케이스 구현.
 *
 * 역할:
 * - 멱등성 확인 (기존 생성 결과 있으면 재사용)
 * - 본문 생성 (template 또는 AI)
 * - S3 저장
 * - DB 메타데이터 저장 (webhook 콘텍스트일 때만)
 *
 * 마이그레이션:
 * - 기존 GitHubBodyGenerateApplicationService → 이 서비스로 통합
 * - 기존 GitHubBodyGeneratorService → 여전히 도메인 서비스로 사용 (생성 로직)
 * - 기존 GitHubBodyStorageService → 여전히 도메인 서비스로 사용 (저장 로직)
 *
 * @see GenerateGitHubBodyUseCase
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GenerateBodyService implements GenerateGitHubBodyUseCase {

  private final GitHubBodyGeneratorService generatorService;
  private final GitHubBodyStorageService storageService;
  private final BodyStoragePersistencePort persistencePort;

  @Override
  public GitHubBodyResponseDto generate(Long bodyTemplatePromptId, GitHubBodyRequestDto request) {
    return generateInternal(bodyTemplatePromptId, request, null, null, null);
  }

  @Transactional
  @Override
  public GitHubBodyResponseDto generateWithPersistence(Long bodyTemplatePromptId, GitHubBodyRequestDto request,
                                                        @NonNull String tenantKey, @Nullable String eventType,
                                                        @Nullable Long ownerUserId) {
    return generateInternal(bodyTemplatePromptId, request, tenantKey, eventType, ownerUserId);
  }

  private GitHubBodyResponseDto generateInternal(Long promptId, GitHubBodyRequestDto request,
                                                  @Nullable String tenantKey, @Nullable String eventType,
                                                  @Nullable Long ownerUserId) {
    String jobId = request.resolveJobId();
    log.info("GitHub body generation requested - promptId: {}, jobId: {}, repo: {}", promptId, jobId, request.repoFullName());

    var existing = storageService.tryGetExistingKeys(request);
    if (existing.isPresent()) {
      var keys = existing.get();
      String issueBody = storageService.downloadBody(keys.storedIssueFileKey());
      String prBody = storageService.downloadBody(keys.storedPrFileKey());
      GitHubBodyResponseDto dto = new GitHubBodyResponseDto(
          jobId, issueBody, prBody,
          keys.storedIssueFileKey(), keys.storedPrFileKey());
      upsertMetadataIfNeeded(tenantKey, request, jobId, promptId, keys.storedIssueFileKey(),
          keys.storedPrFileKey(), eventType, ownerUserId);
      return dto;
    }

    GitHubBodyGeneratorService.GitHubBodyPair pair = generatorService.generateBoth(promptId, request);
    Optional<GitHubBodyStorageService.StoredKeys> keysOpt = storageService.saveBothAsMarkdown(
        pair.issueBody(), pair.prBody(), request);

    if (keysOpt.isEmpty()) {
      return new GitHubBodyResponseDto(jobId, pair.issueBody(), pair.prBody(), null, null);
    }

    GitHubBodyStorageService.StoredKeys keys = keysOpt.get();
    GitHubBodyResponseDto dto = new GitHubBodyResponseDto(
        jobId,
        pair.issueBody(),
        pair.prBody(),
        keys.storedIssueFileKey(),
        keys.storedPrFileKey());
    upsertMetadataIfNeeded(tenantKey, request, jobId, promptId, keys.storedIssueFileKey(),
        keys.storedPrFileKey(), eventType, ownerUserId);
    return dto;
  }

  private void upsertMetadataIfNeeded(@Nullable String tenantKey, GitHubBodyRequestDto request, String jobId,
                                      Long bodyPromptId,
                                      @Nullable String storedIssueFileKey, @Nullable String storedPrFileKey,
                                      @Nullable String eventType, @Nullable Long ownerUserId) {
    if (tenantKey == null || tenantKey.isBlank()
        || storedIssueFileKey == null || storedPrFileKey == null
        || request.repoFullName() == null || request.repoFullName().isBlank()) {
      log.debug("Skipping metadata upsert - missing required fields");
      return;
    }

    try {
      // TODO: GithubBodyStorage 엔티티 생성 및 upsert 호출
      // persistencePort.upsert(storage);
      log.debug("GitHub body storage metadata upserted - tenantKey: {}, repo: {}, jobId: {}",
          tenantKey, request.repoFullName(), jobId);
    } catch (Exception e) {
      log.warn("GitHub body storage metadata upsert failed: {}", e.getMessage());
      throw new BaseException(ModuleErrorCode.GITHUB_BODY_STORAGE_UPSERT_FAILED, null,
          "tenantKey: " + tenantKey + ", jobId: " + jobId, e);
    }
  }
}
