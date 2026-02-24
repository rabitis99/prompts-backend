package org.example.sharedprompts.module.github.application.body;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.example.sharedprompts.module.github.domain.repository.GithubBodyStorageRepository;
import org.example.sharedprompts.module.github.domain.service.GitHubBodyGeneratorService;
import org.example.sharedprompts.module.github.domain.service.GitHubBodyStorageService;
import org.example.sharedprompts.module.github.dto.body.request.GitHubBodyRequestDto;
import org.example.sharedprompts.module.github.dto.body.response.GitHubBodyResponseDto;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * GitHub Issue/PR 본문 생성 오케스트레이션.
 * 멱등 확인 → 기존 키 있으면 다운로드 반환 / 없으면 생성 → S3 저장 → (선택) DB 메타데이터 UPSERT → 응답 생성.
 * 참고: DB UPSERT 실패 시 트랜잭션 롤백으로 DB는 원복되나 S3 파일은 남을 수 있음. 멱등 재시도 시 기존 키 재사용으로 유실은 없음. 장기적으로 S3 고아 파일 정리(TTL/배치) 고려 권장.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubBodyGenerateApplicationService {

    private final GitHubBodyGeneratorService generatorService;
    private final GitHubBodyStorageService storageService;
    private final GithubBodyStorageRepository githubBodyStorageRepository;

    /**
     * 본문 생성 후 S3 저장만 수행. DB 메타데이터는 저장하지 않음 (기존 호출부 호환).
     */
    public GitHubBodyResponseDto generate(Long promptId, GitHubBodyRequestDto request) {
        return generateInternal(promptId, request, null, null, null);
    }

    /**
     * 본문 생성 후 S3 저장 및 DB 메타데이터 UPSERT 수행 (Webhook 등 tenant/event 정보가 있을 때).
     * tenantKey가 null이 아니고 저장된 키가 둘 다 있을 때만 github_body_storage에 UPSERT합니다.
     */
    @Transactional
    public GitHubBodyResponseDto generate(Long promptId, GitHubBodyRequestDto request,
                                          @NonNull String tenantKey, @Nullable String eventType, @Nullable Long ownerUserId) {
        return generateInternal(promptId, request, tenantKey, eventType, ownerUserId);
    }

    private GitHubBodyResponseDto generateInternal(Long promptId, GitHubBodyRequestDto request,
                                                   @Nullable String tenantKey, @Nullable String eventType, @Nullable Long ownerUserId) {
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
            upsertMetadataIfNeeded(tenantKey, request, jobId, promptId, keys.storedIssueFileKey(), keys.storedPrFileKey(), eventType, ownerUserId);
            return dto;
        }

        GitHubBodyGeneratorService.GitHubBodyPair pair = generatorService.generateBoth(promptId, request);
        Optional<GitHubBodyStorageService.StoredKeys> keysOpt = storageService.saveBothAsMarkdown(
                pair.issueBody(), pair.prBody(), request);
        if (keysOpt.isEmpty()) {
            return new GitHubBodyResponseDto(
                    jobId,
                    pair.issueBody(),
                    pair.prBody(),
                    null,
                    null);
        }
        GitHubBodyStorageService.StoredKeys keys = keysOpt.get();
        GitHubBodyResponseDto dto = new GitHubBodyResponseDto(
                jobId,
                pair.issueBody(),
                pair.prBody(),
                keys.storedIssueFileKey(),
                keys.storedPrFileKey());
        upsertMetadataIfNeeded(tenantKey, request, jobId, promptId, keys.storedIssueFileKey(), keys.storedPrFileKey(), eventType, ownerUserId);
        return dto;
    }

    /**
     * tenantKey가 있고 저장된 이슈/PR 키가 둘 다 null이 아닐 때만 (tenant_key, repo_full_name, job_id) 기준 UPSERT.
     */
    private void upsertMetadataIfNeeded(@Nullable String tenantKey, GitHubBodyRequestDto request, String jobId,
                                        Long bodyPromptId,
                                        @Nullable String storedIssueFileKey, @Nullable String storedPrFileKey,
                                        @Nullable String eventType, @Nullable Long ownerUserId) {
        if (tenantKey == null || tenantKey.isBlank()
                || storedIssueFileKey == null || storedPrFileKey == null
                || request.repoFullName() == null || request.repoFullName().isBlank()) {
            log.debug("Skipping metadata upsert - missing required fields (tenantKey/repoFullName/storageKeys)");
            return;
        }
        String repoFullName = request.repoFullName();
        String deliveryId = request.deliveryId() != null && !request.deliveryId().isBlank() ? request.deliveryId() : null;
        try {
            githubBodyStorageRepository.upsert(
                    tenantKey,
                    repoFullName,
                    jobId,
                    deliveryId,
                    eventType,
                    storedIssueFileKey,
                    storedPrFileKey,
                    bodyPromptId,
                    ownerUserId
            );
            log.debug("GitHub body storage metadata upserted - tenantKey: {}, repo: {}, jobId: {}", tenantKey, repoFullName, jobId);
        } catch (Exception e) {
            log.warn("GitHub body storage metadata upsert failed - tenantKey: {}, jobId: {}: {}", tenantKey, jobId, e.getMessage());
            throw new BaseException(ModuleErrorCode.GITHUB_BODY_STORAGE_UPSERT_FAILED, null,
                    "tenantKey: " + tenantKey + ", jobId: " + jobId, e);
        }
    }
}
