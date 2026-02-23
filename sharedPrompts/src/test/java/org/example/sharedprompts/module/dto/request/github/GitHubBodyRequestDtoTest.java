package org.example.sharedprompts.module.dto.request.github;

import org.example.sharedprompts.module.exception.BaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GitHubBodyRequestDto resolveJobId 우선순위 테스트")
class GitHubBodyRequestDtoTest {

    @ParameterizedTest(name = "jobId={0}, deliveryId={1}, sha={2} → expect {3}")
    @CsvSource({
            "j1, d1, s1, j1",
            "j1, d1, '', j1",
            "j1, '', s1, j1",
            "j1, '', '', j1",
            "'', d1, s1, d1",
            "'', d1, '', d1",
            "'', '', s1, s1",
    })
    @DisplayName("resolveJobId 우선순위: jobId > deliveryId > sha")
    void resolveJobId_priority(String jobId, String deliveryId, String sha, String expected) {
        GitHubBodyRequestDto dto = new GitHubBodyRequestDto(
                blankToNull(jobId), blankToNull(deliveryId), blankToNull(sha),
                null, null, null, null, null, null, null, null, null);
        assertThat(dto.resolveJobId()).isEqualTo(expected);
    }

    @Test
    @DisplayName("jobId, deliveryId, sha 모두 없으면 BaseException")
    void resolveJobId_allMissing_throws() {
        GitHubBodyRequestDto dto = new GitHubBodyRequestDto(
                null, null, null, null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(dto::resolveJobId)
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("jobId, deliveryId, sha 중 하나 이상 필요");
    }

    @Test
    @DisplayName("resolveBaseBranch: null/blank면 main")
    void resolveBaseBranch_defaultMain() {
        GitHubBodyRequestDto dto = new GitHubBodyRequestDto(
                "j", null, null, null, null, null, null, null, null, null, null, null);
        assertThat(dto.resolveBaseBranch()).isEqualTo("main");

        GitHubBodyRequestDto withBase = new GitHubBodyRequestDto(
                "j", null, null, null, null, "develop", null, null, null, null, null, null);
        assertThat(withBase.resolveBaseBranch()).isEqualTo("develop");
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
