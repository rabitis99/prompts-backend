package org.example.sharedprompts.module.domain.github;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GitHubBodyInputTruncator 입력 크기 제한 테스트")
class GitHubBodyInputTruncatorTest {

    @Test
    @DisplayName("commits 50줄 초과 시 앞 50줄 + ... and N more")
    void truncateCommits_over50_truncates() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 60; i++) sb.append("line").append(i).append("\n");
        String result = GitHubBodyInputTruncator.truncateCommits(sb.toString());
        assertThat(result.split("\n").length).isEqualTo(51); // 50 lines + "... and 10 more"
        assertThat(result).contains("... and 10 more");
    }

    @Test
    @DisplayName("files 200줄 초과 시 앞 200줄 + ... and N more")
    void truncateFiles_over200_truncates() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 250; i++) sb.append("f").append(i).append("\n");
        String result = GitHubBodyInputTruncator.truncateFiles(sb.toString());
        assertThat(result.split("\n").length).isEqualTo(201);
        assertThat(result).contains("... and 50 more");
    }

    @Test
    @DisplayName("50줄 이하면 그대로 반환")
    void truncateCommits_underLimit_unchanged() {
        String text = "a\nb\nc";
        assertThat(GitHubBodyInputTruncator.truncateCommits(text)).isEqualTo(text);
    }
}
