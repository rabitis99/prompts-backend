package org.example.sharedprompts.module.domain.github;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.dto.request.github.GitHubBodyRequestDto;
import org.example.sharedprompts.module.dto.response.github.GitHubBodyResponseDto;
import org.springframework.stereotype.Service;

/**
 * GitHub Issue/PR 본문 생성 오케스트레이션.
 * 멱등 확인 → 기존 키 있으면 다운로드 반환 / 없으면 생성 → 저장 → 응답 생성.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubBodyGenerateApplicationService {

    private final GitHubBodyGeneratorService generatorService;
    private final GitHubBodyStorageService storageService;

    public GitHubBodyResponseDto generate(Long promptId, GitHubBodyRequestDto request) {
        String jobId = request.resolveJobId();
        log.info("GitHub body generation requested - promptId: {}, jobId: {}, repo: {}", promptId, jobId, request.repoFullName());

        var existing = storageService.tryGetExistingKeys(request);
        if (existing.isPresent()) {
            var keys = existing.get();
            String issueBody = storageService.downloadBody(keys.storedIssueFileKey());
            String prBody = storageService.downloadBody(keys.storedPrFileKey());
            return new GitHubBodyResponseDto(
                    jobId, issueBody, prBody,
                    keys.storedIssueFileKey(), keys.storedPrFileKey());
        }

        GitHubBodyGeneratorService.GitHubBodyPair pair = generatorService.generateBoth(promptId, request);
        GitHubBodyStorageService.StoredKeys keys = storageService.saveBothAsMarkdown(
                pair.issueBody(), pair.prBody(), request);
        return new GitHubBodyResponseDto(
                jobId,
                pair.issueBody(),
                pair.prBody(),
                keys.storedIssueFileKey(),
                keys.storedPrFileKey());
    }
}
