package org.example.sharedprompts.module.domain.github;

import org.example.sharedprompts.module.domain.production.service.ai.text.TextAiClient;
import org.example.sharedprompts.module.dto.request.github.GitHubBodyRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GitHubBodyGeneratorService 프롬프트 치환 및 generateBoth 테스트")
class GitHubBodyGeneratorServiceTest {

    @Test
    @DisplayName("TextAiClient 없을 때 템플릿 치환으로 Issue/PR 본문 생성")
    void generateBoth_withoutAiClient_usesTemplateSubstitution() {
        GitHubBodyGeneratorService service = new GitHubBodyGeneratorService(null);
        GitHubBodyRequestDto request = new GitHubBodyRequestDto(
                "job-1", "delivery-1", "abc123",
                "owner/repo", "feature/x", "main",
                "feat: add api", "dev", "2025-02-23",
                "commit message line 1", "src/A.java",
                "github");

        GitHubBodyGeneratorService.GitHubBodyPair pair = service.generateBoth(request);

        assertThat(pair.issueBody()).contains("owner/repo").contains("abc123").contains("job-1");
        assertThat(pair.issueBody()).contains("[AUTO_JOB_ID:job-1]").contains("[AUTO_PUSH_DELIVERY:delivery-1]").contains("[AUTO_PUSH_SHA:abc123]");
        assertThat(pair.prBody()).contains("owner/repo").contains("main").contains("feature/x");
        assertThat(pair.prBody()).contains("[AUTO_JOB_ID:job-1]");
    }

    @Test
    @DisplayName("Mock Groq 응답 시 치환된 본문 반환")
    void generateBody_withMockGroq_substitutesPlaceholders() {
        TextAiClient mockClient = (prompt, modelName, contentTypeHint) ->
                "## TL;DR\n- Summary from AI.\n\n## Links\n- https://github.com/{{REPO}}/commit/{{SHA}}\n\n[AUTO_JOB_ID:{{JOB_ID}}]\n[AUTO_PUSH_DELIVERY:{{DELIVERY_ID}}]\n[AUTO_PUSH_SHA:{{SHA}}]";
        GitHubBodyGeneratorService service = new GitHubBodyGeneratorService(mockClient);
        GitHubBodyRequestDto request = new GitHubBodyRequestDto(
                "job-2", "del-2", "sha-2",
                "org/repo", "main", null,
                "title", "a", "d", "c", "f", null);

        String body = service.generateBody(request, GitHubBodyRequestDto.Kind.ISSUE);

        assertThat(body).contains("org/repo").contains("sha-2").contains("job-2").contains("del-2");
        assertThat(body).contains("[AUTO_JOB_ID:job-2]").contains("[AUTO_PUSH_DELIVERY:del-2]").contains("[AUTO_PUSH_SHA:sha-2]");
    }
}
