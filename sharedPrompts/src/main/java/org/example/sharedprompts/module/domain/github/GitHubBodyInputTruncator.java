package org.example.sharedprompts.module.domain.github;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 입력 크기 제한: commits 최대 50줄, files 최대 200줄.
 * 초과 시 앞부분만 사용하고 "... and N more"로 축약하여 프롬프트 안정성 확보.
 */
public final class GitHubBodyInputTruncator {

    public static final int MAX_COMMIT_LINES = 50;
    public static final int MAX_FILE_LINES = 200;

    private GitHubBodyInputTruncator() {}

    /**
     * 줄 단위로 잘라서 최대 maxLines줄만 사용하고, 초과분이 있으면 "... and N more"를 붙인다.
     * Trailing newline은 제거 후 줄 수를 계산하여 "... and N more"의 N이 실제 초과 줄 수와 일치하도록 한다.
     */
    public static String truncateToLines(String text, int maxLines) {
        if (text == null || text.isBlank()) return "";
        String normalized = text.replaceAll("\\n+$", "");
        String[] lines = normalized.split("\n", -1);
        if (lines.length <= maxLines) return text;
        String head = Arrays.stream(lines).limit(maxLines).collect(Collectors.joining("\n"));
        int more = lines.length - maxLines;
        return head + "\n... and " + more + " more";
    }

    public static String truncateCommits(String commits) {
        return truncateToLines(commits, MAX_COMMIT_LINES);
    }

    public static String truncateFiles(String files) {
        return truncateToLines(files, MAX_FILE_LINES);
    }
}
