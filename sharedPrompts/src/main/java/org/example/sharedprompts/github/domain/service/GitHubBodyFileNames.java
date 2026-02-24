package org.example.sharedprompts.github.domain.service;

/**
 * S3 저장 시 파일명 규칙: 같은 jobId 아래 issue-{jobId}.md, pr-{jobId}.md.
 * 변경 시 이 클래스만 수정하면 되도록 분리.
 * <p>jobId 유효성 검증은 호출부(서비스 레이어)에서 수행하며, 이 클래스는 포맷터 역할만 합니다.
 */
public final class GitHubBodyFileNames {

    private GitHubBodyFileNames() {}

    public static String issueFileName(String jobId) {
        return "issue-" + jobId + ".md";
    }

    public static String prFileName(String jobId) {
        return "pr-" + jobId + ".md";
    }
}
