package org.example.sharedprompts.module.github.domain.service;

import org.example.sharedprompts.module.github.dto.body.request.GitHubBodyRequestDto;

import java.util.Map;

/**
 * 템플릿 치환용 변수 집합. Request에서 한 곳에서만 생성하여 일관성 유지.
 */
public record GitHubBodyVars(
        String repo,
        String baseBranch,
        String branch,
        String sha,
        String title,
        String author,
        String date,
        String commits,
        String files,
        String jobId,
        String deliveryId
) {

    public static GitHubBodyVars from(GitHubBodyRequestDto request) {
        String jobId = request.resolveJobId();
        return new GitHubBodyVars(
                request.repoForTemplate(),
                request.resolveBaseBranch(),
                nullToEmpty(request.branch()),
                request.shaForTemplate(),
                nullToEmpty(request.title()),
                nullToEmpty(request.author()),
                nullToEmpty(request.date()),
                GitHubBodyInputTruncator.truncateCommits(request.commitsText()),
                GitHubBodyInputTruncator.truncateFiles(request.filesText()),
                jobId,
                request.deliveryIdForTemplate()
        );
    }

    /** Placeholder 이름 → 값 Map (GitHubBodyPlaceholderSubstitutor에서 사용). */
    public Map<String, String> toMap() {
        return Map.ofEntries(
                Map.entry("REPO", repo),
                Map.entry("BASE_BRANCH", baseBranch),
                Map.entry("BRANCH", branch),
                Map.entry("SHA", sha),
                Map.entry("TITLE", title),
                Map.entry("AUTHOR", author),
                Map.entry("DATE", date),
                Map.entry("COMMITS", commits),
                Map.entry("FILES", files),
                Map.entry("JOB_ID", jobId),
                Map.entry("DELIVERY_ID", deliveryId)
        );
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}
