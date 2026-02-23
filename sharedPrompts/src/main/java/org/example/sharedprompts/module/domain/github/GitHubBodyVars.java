package org.example.sharedprompts.module.domain.github;

import org.example.sharedprompts.module.dto.request.github.GitHubBodyRequestDto;

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
                entry("REPO", repo),
                entry("BASE_BRANCH", baseBranch),
                entry("BRANCH", branch),
                entry("SHA", sha),
                entry("TITLE", title),
                entry("AUTHOR", author),
                entry("DATE", date),
                entry("COMMITS", commits),
                entry("FILES", files),
                entry("JOB_ID", jobId),
                entry("DELIVERY_ID", deliveryId)
        );
    }

    private static Map.Entry<String, String> entry(String k, String v) {
        return Map.entry(k, v);
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}
