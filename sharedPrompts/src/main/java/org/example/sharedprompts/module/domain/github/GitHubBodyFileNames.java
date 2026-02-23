package org.example.sharedprompts.module.domain.github;

/**
 * S3 저장 시 파일명 규칙: 같은 jobId 아래 issue-{jobId}.md, pr-{jobId}.md.
 * 변경 시 이 클래스만 수정하면 되도록 분리.
 */
public final class GitHubBodyFileNames {

    private GitHubBodyFileNames() {}

    public static String issueFileName(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            throw new IllegalArgumentException("jobId must not be blank");
        }
        return "issue-" + jobId + ".md";
    }

    public static String prFileName(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            throw new IllegalArgumentException("jobId must not be blank");
        }
        return "pr-" + jobId + ".md";
    }
}
